package com.wellheadstone.nemms.web.membership.controllers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.wellheadstone.nemms.common.viewmodel.JsonResult;
import com.wellheadstone.nemms.common.viewmodel.ParamJsonResult;
import com.wellheadstone.nemms.common.viewmodel.TreeNode;
import com.wellheadstone.nemms.data.PageInfo;
import com.wellheadstone.nemms.membership.po.EventPo;
import com.wellheadstone.nemms.membership.po.ModulePo;
import com.wellheadstone.nemms.membership.service.ModuleService;
import com.wellheadstone.nemms.web.DataGridPager;
import com.wellheadstone.nemms.web.controllers.AbstractController;

@Controller
@RequestMapping(value = "/membership/module")
public class ModuleController extends AbstractController {
	@Resource
	private ModuleService moduleService;

	@RequestMapping(value = { "", "/", "/index" })
	public String index() {
		return "membership/module";
	}

	@RequestMapping(value = "/refresh")
	@ResponseBody
	public JsonResult refresh() {
		JsonResult result = new JsonResult(false, "刷新模块列表失败！");
		try {
			this.moduleService.reloadCache();
			this.setSuccessResult(result, "刷新模块列表成功！");
		} catch (Exception ex) {
			this.setExceptionResult(result, ex);
		}
		return result;
	}

	@RequestMapping(value = "/getModuleById")
	@ResponseBody
	public ModulePo getModule(String id) {
		return this.moduleService.getById(id);
	}

	@RequestMapping(value = "/list")
	@ResponseBody
	public Map<String, Object> list(DataGridPager pager, Integer id, HttpServletRequest request) {
		int pid = (id == null ? 0 : id);
		pager.setDefaultSort(EventPo.CreateTime);
		pager.setSort(ModulePo.getColumnName(pager.getSort()));
		PageInfo pageInfo = new PageInfo((pager.getPage() - 1) * pager.getRows(),
				pager.getRows(), pager.getSort(), pager.getOrder());
		List<ModulePo> list = this.moduleService.getDao().queryPageByPid(pageInfo, pid);
		Map<String, Object> modelMap = new HashMap<String, Object>(2);
		modelMap.put("total", pageInfo.getTotals());
		modelMap.put("rows", list);

		return modelMap;
	}

	@RequestMapping(value = "/getChildren")
	@ResponseBody
	public List<TreeNode<ModulePo>> getChildren(Integer id)
	{
		refresh();
		int parentId = (id == null ? 0 : id);
		List<ModulePo> modules = this.moduleService.getChildModules(parentId);
		List<TreeNode<ModulePo>> treeNodes = new ArrayList<TreeNode<ModulePo>>(modules.size());

		for (ModulePo po : modules) {
			String configId = Integer.toString(po.getModuleId());
			String pid = Integer.toString(po.getParentId());
			String text = po.getName();
			TreeNode<ModulePo> vmMode = new TreeNode<ModulePo>(configId, pid, text, "closed", po.getIcon(), false, po);
			treeNodes.add(vmMode);
		}

		return treeNodes;
	}

	@RequestMapping(value = "/add")
	@ResponseBody
	public ParamJsonResult<ModulePo> add(ModulePo po) {
		ParamJsonResult<ModulePo> result = new ParamJsonResult<ModulePo>(false, "添加模块失败！");

		if (StringUtils.isBlank(po.getUrl())) {
			this.setFailureResult(result, "url不能为空！");
			return result;
		}
		try {
			po.setCreateTime(Calendar.getInstance().getTime());
			po.setPath("");
			this.moduleService.addModule(po);
			this.setSuccessResult(result, "添加模块成功！");
			this.moduleService.reloadCache();
		} catch (Exception ex) {
			this.setExceptionResult(result, ex);
		}

		return result;
	}

	@RequestMapping(value = "/edit")
	@ResponseBody
	public ParamJsonResult<ModulePo> edit(ModulePo po) {
		ParamJsonResult<ModulePo> result = new ParamJsonResult<ModulePo>(false, "更新模块失败！");
		String[] names = new String[] {
				ModulePo.Name, ModulePo.Code, ModulePo.Status, ModulePo.Sequence,
				ModulePo.Comment, ModulePo.Url, ModulePo.Icon, ModulePo.LinkType,
				ModulePo.Target, ModulePo.Params
		};

		try {
			this.moduleService.edit(po, po.getModuleId(), names);
			this.setSuccessResult(result, "更新模块成功！");
			this.moduleService.reloadCache();
		} catch (Exception ex) {
			this.setExceptionResult(result, ex);
		}
		return result;
	}

	@RequestMapping(value = "/remove")
	@ResponseBody
	public ParamJsonResult<ModulePo> remove(String id) {
		ParamJsonResult<ModulePo> result = new ParamJsonResult<ModulePo>(false, "删除模块失败");

		try {
			ModulePo module = this.moduleService.getById(id, ModulePo.ParentId);
			this.moduleService.remove(Integer.parseInt(id), module.getParentId());
			this.setSuccessResult(result, "删除模块成功！");
			this.moduleService.reloadCache();
		} catch (Exception ex) {
			this.setExceptionResult(result, ex);
		}

		return result;
	}
}
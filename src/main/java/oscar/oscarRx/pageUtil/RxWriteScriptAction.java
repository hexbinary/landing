/*
 *
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved. *
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. * * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA. *
 *
 * <OSCAR TEAM>
 *
 * This software was written for the
 * Department of Family Medicine
 * McMaster University
 * Hamilton
 * Ontario, Canada
 */
package oscar.oscarRx.pageUtil;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.apache.struts.util.MessageResources;
import org.oscarehr.casemgmt.model.CaseManagementNote;
import org.oscarehr.casemgmt.model.CaseManagementNoteLink;
import org.oscarehr.casemgmt.service.CaseManagementManager;
import org.oscarehr.common.dao.DemographicDao;
import org.oscarehr.common.dao.UserPropertyDAO;
import org.oscarehr.common.model.Demographic;
import org.oscarehr.common.model.UserProperty;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import oscar.log.LogAction;
import oscar.log.LogConst;
import oscar.oscarRx.data.RxDrugData;
import oscar.oscarRx.data.RxPrescriptionData;
import oscar.oscarRx.util.RxUtil;

public final class RxWriteScriptAction extends DispatchAction {

	private static final Logger logger = MiscUtils.getLogger();
	private static UserPropertyDAO userPropertyDAO;
	private static final String DEFAULT_QUANTITY = "30";

	public ActionForward unspecified(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, Exception {

		RxWriteScriptForm frm = (RxWriteScriptForm) form;
		String fwd = "refresh";
		oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) request.getSession().getAttribute("RxSessionBean");

		if (bean == null) {
			response.sendRedirect("error.html");
			return null;
		}

		if (frm.getAction().startsWith("update")) {

			RxDrugData drugData = new RxDrugData();
			RxPrescriptionData.Prescription rx = bean.getStashItem(bean.getStashIndex());
			RxPrescriptionData prescription = new RxPrescriptionData();

			if (frm.getGCN_SEQNO() != 0) { // not custom
				if (frm.getBrandName().equals(rx.getBrandName()) == false) {
					rx.setBrandName(frm.getBrandName());
				} else {
					rx.setGCN_SEQNO(frm.getGCN_SEQNO());
				}
			} else { // custom
				rx.setBrandName(null);
				rx.setGCN_SEQNO(0);
				rx.setCustomName(frm.getCustomName());
			}

			rx.setRxDate(RxUtil.StringToDate(frm.getRxDate(), "yyyy-MM-dd"));
			rx.setWrittenDate(RxUtil.StringToDate(frm.getWrittenDate(), "yyyy-MM-dd"));
			rx.setTakeMin(frm.getTakeMinFloat());
			rx.setTakeMax(frm.getTakeMaxFloat());
			rx.setFrequencyCode(frm.getFrequencyCode());
			rx.setDuration(frm.getDuration());
			rx.setDurationUnit(frm.getDurationUnit());
			rx.setQuantity(frm.getQuantity());
			rx.setRepeat(frm.getRepeat());
			rx.setLastRefillDate(RxUtil.StringToDate(frm.getLastRefillDate(), "yyyy-MM-dd"));
			rx.setNosubs(frm.getNosubs());
			rx.setPrn(frm.getPrn());
			rx.setSpecial(frm.getSpecial());
			rx.setAtcCode(frm.getAtcCode());
			rx.setRegionalIdentifier(frm.getRegionalIdentifier());
			rx.setUnit(frm.getUnit());
			rx.setUnitName(frm.getUnitName());
			rx.setMethod(frm.getMethod());
			rx.setRoute(frm.getRoute());
			rx.setCustomInstr(frm.getCustomInstr());
			rx.setDosage(frm.getDosage());
			rx.setOutsideProviderName(frm.getOutsideProviderName());
			rx.setOutsideProviderOhip(frm.getOutsideProviderOhip());
			rx.setLongTerm(frm.getLongTerm());
			rx.setPastMed(frm.getPastMed());
			rx.setPatientCompliance(frm.getPatientComplianceY(), frm.getPatientComplianceN());

			try {
				rx.setDrugForm(drugData.getDrugForm(String.valueOf(frm.getGCN_SEQNO())));
			} catch (Exception e) {
				logger.error("Unable to get DrugForm from drugref");
			}

			logger.debug("SAVING STASH " + rx.getCustomInstr());
			if (rx.getSpecial() == null) {
				logger.error("Drug.special is null : " + rx.getSpecial() + " : " + frm.getSpecial());
			} else if (rx.getSpecial().length() < 6) {
				logger.warn("Drug.special appears to be empty : " + rx.getSpecial() + " : " + frm.getSpecial());
			}

			String annotation_attrib = request.getParameter("annotation_attrib");
			if (annotation_attrib == null) {
				annotation_attrib = "";
			}

			bean.addAttributeName(annotation_attrib, bean.getStashIndex());
			bean.setStashItem(bean.getStashIndex(), rx);
			rx = null;

			if (frm.getAction().equals("update")) {
				fwd = "refresh";
			}
			if (frm.getAction().equals("updateAddAnother")) {
				fwd = "addAnother";
			}
			if (frm.getAction().equals("updateAndPrint")) {
				// SAVE THE DRUG
				int i;
				String scriptId = prescription.saveScript(bean);
				@SuppressWarnings("unchecked")
				ArrayList<String> attrib_names = bean.getAttributeNames();
				// p("attrib_names", attrib_names.toString());
				StringBuilder auditStr = new StringBuilder();
				for (i = 0; i < bean.getStashSize(); i++) {
					rx = bean.getStashItem(i);

					rx.Save(scriptId);
					auditStr.append(rx.getAuditString());
					auditStr.append("\n");

					/* Save annotation */
					HttpSession se = request.getSession();
					WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(se.getServletContext());
					CaseManagementManager cmm = (CaseManagementManager) ctx.getBean("caseManagementManager");
					String attrib_name = attrib_names.get(i);
					if (attrib_name != null) {
						CaseManagementNote cmn = (CaseManagementNote) se.getAttribute(attrib_name);
						if (cmn != null) {
							cmm.saveNoteSimple(cmn);
							CaseManagementNoteLink cml = new CaseManagementNoteLink();
							cml.setTableName(CaseManagementNoteLink.DRUGS);
							cml.setTableId((long) rx.getDrugId());
							cml.setNoteId(cmn.getId());
							cmm.saveNoteLink(cml);
							se.removeAttribute(attrib_name);
							LogAction.addLog(cmn.getProviderNo(), LogConst.ANNOTATE, CaseManagementNoteLink.DISP_PRESCRIP, scriptId, request.getRemoteAddr(), cmn.getDemographic_no(), cmn.getNote());
						}
					}
					rx = null;
				}
				fwd = "viewScript";
				String ip = request.getRemoteAddr();
				request.setAttribute("scriptId", scriptId);
				LogAction.addLog((String) request.getSession().getAttribute("user"), LogConst.ADD, LogConst.CON_PRESCRIPTION, scriptId, ip, "" + bean.getDemographicNo(), auditStr.toString());
			}
		}
		return mapping.findForward(fwd);
	}

	public ActionForward updateReRxDrug(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) request.getSession().getAttribute("RxSessionBean");
		if (bean == null) {
			response.sendRedirect("error.html");
			return null;
		}
		List<String> reRxDrugIdList = bean.getReRxDrugIdList();
		String action = request.getParameter("action");
		String drugId = request.getParameter("reRxDrugId");
		if (action.equals("addToReRxDrugIdList") && !reRxDrugIdList.contains(drugId)) {
			reRxDrugIdList.add(drugId);
		} else if (action.equals("removeFromReRxDrugIdList") && reRxDrugIdList.contains(drugId)) {
			reRxDrugIdList.remove(drugId);
		} else if (action.equals("clearReRxDrugIdList")) {
			bean.clearReRxDrugIdList();
		} else {
			logger.warn("WARNING: reRxDrugId not updated");
		}

		return null;

	}

	public ActionForward saveCustomName(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) request.getSession().getAttribute("RxSessionBean");
		if (bean == null) {
			response.sendRedirect("error.html");
			return null;
		}
		try {
			String randomId = request.getParameter("randomId");
			String customName = request.getParameter("customName");
			RxPrescriptionData.Prescription rx = bean.getStashItem2(Integer.parseInt(randomId));
			if (rx == null) {
				logger.error("rx is null", new NullPointerException());
				return null;
			}
			rx.setCustomName(customName);
			rx.setBrandName(null);
			rx.setGenericName(null);
			bean.setStashItem(bean.getIndexFromRx(Integer.parseInt(randomId)), rx);

		} catch (Exception e) {
			logger.error("Error", e);
		}

		return null;
	}

	private void setDefaultQuantity(final HttpServletRequest request) {
		try {
			WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getSession().getServletContext());
			String provider = (String) request.getSession().getAttribute("user");
			if (provider != null) {
				userPropertyDAO = (UserPropertyDAO) ctx.getBean("UserPropertyDAO");
				UserProperty prop = userPropertyDAO.getProp(provider, UserProperty.RX_DEFAULT_QUANTITY);
				if (prop != null) RxUtil.setDefaultQuantity(prop.getValue());
				else RxUtil.setDefaultQuantity(DEFAULT_QUANTITY);
			} else {
				logger.error("Provider is null", new NullPointerException());
			}
		} catch (Exception e) {
			logger.error("Error", e);
		}
	}

	private RxPrescriptionData.Prescription setCustomRxDurationQuantity(RxPrescriptionData.Prescription rx) {
		String quantity = rx.getQuantity();
		if (RxUtil.isMitte(quantity)) {
			String duration = RxUtil.getDurationFromQuantityText(quantity);
			String durationUnit = RxUtil.getDurationUnitFromQuantityText(quantity);
			rx.setDuration(duration);
			rx.setDurationUnit(durationUnit);
			rx.setQuantity(RxUtil.getQuantityFromQuantityText(quantity));
			rx.setUnitName(RxUtil.getUnitNameFromQuantityText(quantity));// this is actually an indicator for Mitte rx
		} else rx.setDuration(RxUtil.findDuration(rx));

		return rx;
	}

	public ActionForward newCustomNote(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		logger.debug("=============Start newCustomNote RxWriteScriptAction.java===============");

		oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) request.getSession().getAttribute("RxSessionBean");
		if (bean == null) {
			response.sendRedirect("error.html");
			return null;
		}

		try {
			RxPrescriptionData rxData = new RxPrescriptionData();

			// create Prescription
			RxPrescriptionData.Prescription rx = rxData.newPrescription(bean.getProviderNo(), bean.getDemographicNo());
			String ra = request.getParameter("randomId");
			rx.setRandomId(Integer.parseInt(ra));
			rx.setCustomNote(true);
			rx.setGenericName(null);
			rx.setBrandName(null);
			rx.setDrugForm("");
			rx.setRoute("");
			rx.setDosage("");
			rx.setUnit("");
			rx.setGCN_SEQNO(0);
			rx.setRegionalIdentifier("");
			rx.setAtcCode("");
			RxUtil.setDefaultSpecialQuantityRepeat(rx);
			rx = setCustomRxDurationQuantity(rx);
			bean.addAttributeName(rx.getAtcCode() + "-" + String.valueOf(bean.getStashIndex()));
			List<RxPrescriptionData.Prescription> listRxDrugs = new ArrayList();

			if (RxUtil.isRxUniqueInStash(bean, rx)) {
				listRxDrugs.add(rx);
			}
			int rxStashIndex = bean.addStashItem(rx);
			bean.setStashIndex(rxStashIndex);

			String today = null;
			Calendar calendar = Calendar.getInstance();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			try {
				today = dateFormat.format(calendar.getTime());
				// p("today's date", today);
			} catch (Exception e) {
				logger.error("Error", e);
			}
			Date tod = RxUtil.StringToDate(today, "yyyy-MM-dd");
			rx.setRxDate(tod);
			rx.setWrittenDate(tod);

			request.setAttribute("listRxDrugs", listRxDrugs);
		} catch (Exception e) {
			logger.error("Error", e);
		}
		logger.debug("=============END newCustomNote RxWriteScriptAction.java===============");
		return (mapping.findForward("newRx"));
	}

	public ActionForward listPreviousInstructions(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		logger.debug("=============Start listPreviousInstructions RxWriteScriptAction.java===============");
		String randomId = request.getParameter("randomId");
		randomId = randomId.trim();
		// get rx from randomId.
		// if rx is normal drug, if din is not null, use din to find it
		// if din is null, use BN to find it
		// if rx is custom drug, use customName to find it.
		// append results to a list.
		RxSessionBean bean = (RxSessionBean) request.getSession().getAttribute("RxSessionBean");
		if (bean == null) {
			response.sendRedirect("error.html");
			return null;
		}
		// create Prescription
		RxPrescriptionData.Prescription rx = bean.getStashItem2(Integer.parseInt(randomId));
		List<HashMap<String, String>> retList = new ArrayList();
		retList = RxUtil.getPreviousInstructions(rx);

		bean.setListMedHistory(retList);
		return null;
	}

	public ActionForward newCustomDrug(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		logger.debug("=============Start newCustomDrug RxWriteScriptAction.java===============");

		MessageResources messages = getResources(request);
		// set default quantity;
		setDefaultQuantity(request);

		oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) request.getSession().getAttribute("RxSessionBean");
		if (bean == null) {
			response.sendRedirect("error.html");
			return null;
		}

		try {
			RxPrescriptionData rxData = new RxPrescriptionData();

			// create Prescription
			RxPrescriptionData.Prescription rx = rxData.newPrescription(bean.getProviderNo(), bean.getDemographicNo());
			String ra = request.getParameter("randomId");
			rx.setRandomId(Integer.parseInt(ra));
			rx.setGenericName(null);
			rx.setBrandName(null);
			rx.setDrugForm("");
			rx.setRoute("");
			rx.setDosage("");
			rx.setUnit("");
			rx.setGCN_SEQNO(0);
			rx.setRegionalIdentifier("");
			rx.setAtcCode("");
			RxUtil.setDefaultSpecialQuantityRepeat(rx);// 1 OD, 20, 0;
			rx = setCustomRxDurationQuantity(rx);
			bean.addAttributeName(rx.getAtcCode() + "-" + String.valueOf(bean.getStashIndex()));
			List<RxPrescriptionData.Prescription> listRxDrugs = new ArrayList();

			if (RxUtil.isRxUniqueInStash(bean, rx)) {
				listRxDrugs.add(rx);
			}
			int rxStashIndex = bean.addStashItem(rx);
			bean.setStashIndex(rxStashIndex);

			String today = null;
			Calendar calendar = Calendar.getInstance();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			try {
				today = dateFormat.format(calendar.getTime());
				// p("today's date", today);
			} catch (Exception e) {
				logger.error("Error", e);
			}
			Date tod = RxUtil.StringToDate(today, "yyyy-MM-dd");
			rx.setRxDate(tod);
			rx.setWrittenDate(tod);

			request.setAttribute("listRxDrugs", listRxDrugs);
		} catch (Exception e) {
			logger.error("Error", e);
		}
		return (mapping.findForward("newRx"));
	}

	public ActionForward normalDrugSetCustom(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) request.getSession().getAttribute("RxSessionBean");
		if (bean == null) {
			response.sendRedirect("error.html");
			return null;
		}
		String randomId = request.getParameter("randomId");
		String customDrugName = request.getParameter("customDrugName");
		logger.debug("radomId=" + randomId);
		if (randomId != null && customDrugName != null) {
			RxPrescriptionData.Prescription normalRx = bean.getStashItem2(Integer.parseInt(randomId));
			if (normalRx != null) {// set other fields same as normal drug, set some fields null like custom drug, remove normal drugfrom stash,add customdrug to stash,
				// forward to prescribe.jsp
				RxPrescriptionData rxData = new RxPrescriptionData();
				RxPrescriptionData.Prescription customRx = rxData.newPrescription(bean.getProviderNo(), bean.getDemographicNo());
				customRx = normalRx;
				customRx.setCustomName(customDrugName);
				customRx.setRandomId(Long.parseLong(randomId));
				customRx.setGenericName(null);
				customRx.setBrandName(null);
				customRx.setDrugForm("");
				customRx.setRoute("");
				customRx.setDosage("");
				customRx.setUnit("");
				customRx.setGCN_SEQNO(0);
				customRx.setRegionalIdentifier("");
				customRx.setAtcCode("");
				bean.setStashItem(bean.getIndexFromRx(Integer.parseInt(randomId)), customRx);
				List<RxPrescriptionData.Prescription> listRxDrugs = new ArrayList();
				if (RxUtil.isRxUniqueInStash(bean, customRx)) {
					// p("unique");
					listRxDrugs.add(customRx);
				}
				request.setAttribute("listRxDrugs", listRxDrugs);
				return (mapping.findForward("newRx"));
			} else {

				return null;
			}
		} else {

			return null;
		}
	}

	public ActionForward createNewRx(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		logger.debug("=============Start createNewRx RxWriteScriptAction.java===============");
		// set default quantity
		setDefaultQuantity(request);

		oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) request.getSession().getAttribute("RxSessionBean");
		if (bean == null) {
			response.sendRedirect("error.html");
			return null;
		}

		try {
			RxPrescriptionData rxData = new RxPrescriptionData();
			RxDrugData drugData = new RxDrugData();

			// create Prescription
			RxPrescriptionData.Prescription rx = rxData.newPrescription(bean.getProviderNo(), bean.getDemographicNo());

			String ra = request.getParameter("randomId");
			int randomId = Integer.parseInt(ra);
			rx.setRandomId(randomId);
			String drugId = request.getParameter("drugId");
			String text = request.getParameter("text");

			// TODO: Is this to slow to do here? It's possible to do this in ajax, as in when this comes back launch an ajax request to fill in.
			RxDrugData.DrugMonograph dmono = drugData.getDrug2(drugId);

			String brandName = text;

			rx.setGenericName(dmono.name); // TODO: how was this done before?
			rx.setBrandName(brandName);

			rx.setDrugForm(dmono.drugForm);

			// TO DO: cache the most used route from the drugs table.
			// for now, check to see if ORAL present, if yes use that, if not use the first one.
			boolean oral = false;
			for (int i = 0; i < dmono.route.size(); i++) {
				if (((String) dmono.route.get(i)).equalsIgnoreCase("ORAL")) {
					oral = true;
				}
			}
			if (oral) {
				rx.setRoute("ORAL");
			} else {
				if (dmono.route.size() > 0) {
					rx.setRoute((String) dmono.route.get(0));
				}
			}
			// if user specified route in instructions, it'll be changed to the one specified.
			String dosage = "";
			String unit = "";
			Vector comps = (Vector) dmono.components;
			for (int i = 0; i < comps.size(); i++) {
				RxDrugData.DrugMonograph.DrugComponent drugComp = (RxDrugData.DrugMonograph.DrugComponent) comps.get(i);
				String strength = drugComp.strength;
				unit = drugComp.unit;
				dosage = dosage + " " + strength + " " + unit;// get drug dosage from strength and unit.
			}
			rx.setDosage(dosage);
			rx.setUnit(unit);
			rx.setGCN_SEQNO(Integer.parseInt(drugId));
			rx.setRegionalIdentifier(dmono.regionalIdentifier);
			String atcCode = dmono.atc;
			rx.setAtcCode(atcCode);
			RxUtil.setSpecialQuantityRepeat(rx);
			rx = setCustomRxDurationQuantity(rx);
			List<RxPrescriptionData.Prescription> listRxDrugs = new ArrayList();
			if (RxUtil.isRxUniqueInStash(bean, rx)) {
				listRxDrugs.add(rx);
			}
			bean.addAttributeName(rx.getAtcCode() + "-" + String.valueOf(bean.getStashIndex()));
			int rxStashIndex = bean.addStashItem(rx);
			bean.setStashIndex(rxStashIndex);
			String today = null;
			Calendar calendar = Calendar.getInstance();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			try {
				today = dateFormat.format(calendar.getTime());
			} catch (Exception e) {
				logger.error("Error", e);
			}
			Date tod = RxUtil.StringToDate(today, "yyyy-MM-dd");
			rx.setRxDate(tod);
			rx.setWrittenDate(tod);
			rx.setDiscontinuedLatest(RxUtil.checkDiscontinuedBefore(rx));// check and set if rx was discontinued before.
			request.setAttribute("listRxDrugs", listRxDrugs);
		} catch (Exception e) {
			logger.error("Error", e);
		}
		logger.debug("=============END createNewRx RxWriteScriptAction.java===============");
		return (mapping.findForward("newRx"));
	}

	public ActionForward updateDrug(ActionMapping mapping, ActionForm aform, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) request.getSession().getAttribute("RxSessionBean");
		if (bean == null) {
			response.sendRedirect("error.html");
			return null;
		}

		String action = request.getParameter("action");

		if (action != null && action.equals("parseInstructions")) {

			try {
				String randomId = request.getParameter("randomId");
				// p("randomId from request",randomId);
				RxPrescriptionData.Prescription rx = bean.getStashItem2(Integer.parseInt(randomId));
				if (rx == null) {
					logger.error("rx is null", new NullPointerException());
				}

				String instructions = request.getParameter("instruction");
				logger.debug("instruction:"+instructions);
				rx.setSpecial(instructions);
				RxUtil.instrucParser(rx);
				bean.addAttributeName(rx.getAtcCode() + "-" + String.valueOf(bean.getIndexFromRx(Integer.parseInt(randomId))));
				bean.setStashItem(bean.getIndexFromRx(Integer.parseInt(randomId)), rx);
				HashMap hm = new HashMap();

				if (rx.getRoute() == null || rx.getRoute().equalsIgnoreCase("null")) {
					rx.setRoute("");
				}

				hm.put("method", rx.getMethod());
				hm.put("takeMin", rx.getTakeMin());
				hm.put("takeMax", rx.getTakeMax());
				hm.put("duration", rx.getDuration());
				hm.put("frequency", rx.getFrequencyCode());
				hm.put("route", rx.getRoute());
				hm.put("durationUnit", rx.getDurationUnit());
				hm.put("prn", rx.getPrn());
				hm.put("calQuantity", rx.getQuantity());
				hm.put("unitName", rx.getUnitName());
				JSONObject jsonObject = JSONObject.fromObject(hm);
				logger.debug("jsonObject:"+jsonObject.toString());
				response.getOutputStream().write(jsonObject.toString().getBytes());
			} catch (Exception e) {
				logger.error("Error", e);
			}
			return null;
		} else if (action != null && action.equals("updateQty")) {
			try {
				String quantity = request.getParameter("quantity");
				String randomId = request.getParameter("randomId");
				RxPrescriptionData.Prescription rx = bean.getStashItem2(Integer.parseInt(randomId));
				// get rx from randomId
				if (quantity == null || quantity.equalsIgnoreCase("null")) {
					quantity = "";
				}
				// check if quantity is same as rx.getquantity(), if yes, do nothing.
				if (quantity.equals(rx.getQuantity()) && rx.getUnitName() == null) {
					// do nothing
				} else {

					if (RxUtil.isStringToNumber(quantity)) {
						rx.setQuantity(quantity);
						rx.setUnitName(null);
					} else if (RxUtil.isMitte(quantity)) {// set duration for mitte

						String duration = RxUtil.getDurationFromQuantityText(quantity);
						String durationUnit = RxUtil.getDurationUnitFromQuantityText(quantity);
						rx.setDuration(duration);
						rx.setDurationUnit(durationUnit);
						rx.setQuantity(RxUtil.getQuantityFromQuantityText(quantity));
						rx.setUnitName(RxUtil.getUnitNameFromQuantityText(quantity));// this is actually an indicator for Mitte rx
					} else {
						rx.setQuantity(RxUtil.getQuantityFromQuantityText(quantity));
						rx.setUnitName(RxUtil.getUnitNameFromQuantityText(quantity));
					}

					String frequency = rx.getFrequencyCode();
					String takeMin = rx.getTakeMinString();
					String takeMax = rx.getTakeMaxString();
					String durationUnit = rx.getDurationUnit();
					double nPerDay = 0d;
					double nDays = 0d;
					if (rx.getUnitName() != null || takeMin.equals("0") || takeMax.equals("0") || frequency.equals("")) {
					} else {
						if (durationUnit.equals("")) {
							durationUnit = "D";
						}

						nPerDay = RxUtil.findNPerDay(frequency);
						nDays = RxUtil.findNDays(durationUnit);
						if (RxUtil.isStringToNumber(quantity) && !rx.isDurationSpecifiedByUser()) {// don't not caculate duration if it's already specified by the user
							double qtyD = Double.parseDouble(quantity);
							// quantity=takeMax * nDays * duration * nPerDay
							double durD = qtyD / ((Double.parseDouble(takeMax)) * nPerDay * nDays);
							int durI = (int) durD;
							rx.setDuration(Integer.toString(durI));
						} else {
							// don't calculate duration if quantity can't be parsed to string
						}
						rx.setDurationUnit(durationUnit);
					}
					// duration=quantity divide by no. of pills per duration period.
					// if not, recalculate duration based on frequency if frequency is not empty
					// if there is already a duration uni present, use that duration unit. if not, set duration unit to days, and output duration in days
				}
				bean.addAttributeName(rx.getAtcCode() + "-" + String.valueOf(bean.getIndexFromRx(Integer.parseInt(randomId))));
				bean.setStashItem(bean.getIndexFromRx(Integer.parseInt(randomId)), rx);
				// RxUtil.printStashContent(bean);
				if (rx.getRoute() == null) {
					rx.setRoute("");
				}
				HashMap hm = new HashMap();
				hm.put("method", rx.getMethod());
				hm.put("takeMin", rx.getTakeMin());
				hm.put("takeMax", rx.getTakeMax());
				hm.put("duration", rx.getDuration());
				hm.put("frequency", rx.getFrequencyCode());
				hm.put("route", rx.getRoute());
				hm.put("durationUnit", rx.getDurationUnit());
				hm.put("prn", rx.getPrn());
				hm.put("calQuantity", rx.getQuantity());
				hm.put("unitName", rx.getUnitName());
				JSONObject jsonObject = JSONObject.fromObject(hm);
				response.getOutputStream().write(jsonObject.toString().getBytes());
			} catch (Exception e) {
				logger.error("Error", e);
			}
			return null;
		} else {
			return null;
		}
	}

	public ActionForward iterateStash(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, Exception {
		oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) request.getSession().getAttribute("RxSessionBean");
		List<RxPrescriptionData.Prescription> listP = Arrays.asList(bean.getStash());
		if (listP.size() == 0) {
			return null;
		} else {
			request.setAttribute("listRxDrugs", listP);
			return (mapping.findForward("newRx"));
		}

	}

	public ActionForward updateSpecialInstruction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, Exception {
		// get special instruction from parameter
		// get rx from random Id
		// rx.setspecialisntruction
		String randomId = request.getParameter("randomId");
		String specialInstruction = request.getParameter("specialInstruction");
		oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) request.getSession().getAttribute("RxSessionBean");
		RxPrescriptionData.Prescription rx = bean.getStashItem2(Integer.parseInt(randomId));
		if (specialInstruction.trim().length() > 0 && !specialInstruction.trim().equalsIgnoreCase("Enter Special Instruction")) {
			rx.setSpecialInstruction(specialInstruction.trim());
		} else {
			rx.setSpecialInstruction(null);
		}

		return null;
	}

	public ActionForward updateProperty(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, Exception {
		oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) request.getSession().getAttribute("RxSessionBean");
		String elem = request.getParameter("elementId");
		String val = request.getParameter("propertyValue");
		val = val.trim();
		if (elem != null && val != null) {
			String[] strArr = elem.split("_");
			if (strArr.length > 1) {
				String num = strArr[1];
				num = num.trim();
				RxPrescriptionData.Prescription rx = bean.getStashItem2(Integer.parseInt(num));
				if (elem.equals("method_" + num)) {
					if (!val.equals("") && !val.equalsIgnoreCase("null")) rx.setMethod(val);
				} else if (elem.equals("route_" + num)) {
					if (!val.equals("") && !val.equalsIgnoreCase("null")) rx.setRoute(val);
				} else if (elem.equals("frequency_" + num)) {
					if (!val.equals("") && !val.equalsIgnoreCase("null")) rx.setFrequencyCode(val);
				} else if (elem.equals("minimum_" + num)) {
					if (!val.equals("") && !val.equalsIgnoreCase("null")) rx.setTakeMin(Float.parseFloat(val));
				} else if (elem.equals("maximum_" + num)) {
					if (!val.equals("") && !val.equalsIgnoreCase("null")) rx.setTakeMax(Float.parseFloat(val));
				} else if (elem.equals("duration_" + num)) {
					if (!val.equals("") && !val.equalsIgnoreCase("null")) rx.setDuration(val);
				} else if (elem.equals("durationUnit_" + num)) {
					if (!val.equals("") && !val.equalsIgnoreCase("null")) rx.setDurationUnit(val);
				} else if (elem.equals("prnVal_" + num)) {
					if (!val.equals("") && !val.equalsIgnoreCase("null")) {
						if (val.equalsIgnoreCase("true")) rx.setPrn(true);
						else rx.setPrn(false);
					} else rx.setPrn(false);
				}
			}
		}
		return null;
	}

	public ActionForward updateSaveAllDrugs(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, Exception {
		oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) request.getSession().getAttribute("RxSessionBean");
		request.getSession().setAttribute("rePrint", null);// set to print.
		List<String> paramList = new ArrayList();
		Enumeration em = request.getParameterNames();
		List<String> randNum = new ArrayList();
		while (em.hasMoreElements()) {
			String ele = em.nextElement().toString();
			paramList.add(ele);
			if (ele.startsWith("drugName_")) {
				String rNum = ele.substring(9);
				if (!randNum.contains(rNum)) {
					randNum.add(rNum);
				}
			}
		}

		List<Integer> allIndex = new ArrayList();
		for (int i = 0; i < bean.getStashSize(); i++) {
			allIndex.add(i);
		}
		List<Integer> existingIndex = new ArrayList();
		for (String num : randNum) {
			int stashIndex = bean.getIndexFromRx(Integer.parseInt(num));
			try {
				if (stashIndex == -1) {
					continue;
				} else {
					existingIndex.add(stashIndex);
					RxPrescriptionData.Prescription rx = bean.getStashItem(stashIndex);

					boolean patientComplianceY = false;
					boolean patientComplianceN = false;
					boolean isOutsideProvider = false;
					boolean isLongTerm = false;
					boolean isPastMed = false;

					em = request.getParameterNames();
					while (em.hasMoreElements()) {
						String elem = (String) em.nextElement();
						String val = request.getParameter(elem);
						val = val.trim();
						if (elem.startsWith("drugName_" + num)) {
							if (rx.isCustom()) {
								rx.setCustomName(val);
								rx.setBrandName(null);
								rx.setGenericName(null);
							} else {
								rx.setBrandName(val);
							}
							;
						} else if (elem.equals("repeats_" + num)) {
							if (val.equals("") || val == null) {
								rx.setRepeat(0);
							} else {
								rx.setRepeat(Integer.parseInt(val));
							}

						} else if (elem.equals("instructions_" + num)) {
							rx.setSpecial(val);
						} else if (elem.equals("quantity_" + num)) {
							if (val.equals("") || val == null) {
								rx.setQuantity("0");
							} else {
								if (RxUtil.isStringToNumber(val)) {
									rx.setQuantity(val);
									rx.setUnitName(null);
								} else {
									rx.setQuantity(RxUtil.getQuantityFromQuantityText(val));
									rx.setUnitName(RxUtil.getUnitNameFromQuantityText(val));
								}
							}
						} else if (elem.equals("longTerm_" + num)) {
							if (val.equals("on")) {
								isLongTerm = true;
							} else {
								isLongTerm = false;
							}
						} else if (elem.equals("lastRefillDate_" + num)) {
							rx.setLastRefillDate(RxUtil.StringToDate(val, "yyyy-MM-dd"));
						} else if (elem.equals("outsideProviderName_" + num)) {
							rx.setOutsideProviderName(val);
						} else if (elem.equals("rxDate_" + num)) {
							if ((val == null) || (val.equals(""))) {
								rx.setRxDate(RxUtil.StringToDate("0000-00-00", "yyyy-MM-dd"));
							} else {
								rx.setRxDate(RxUtil.StringToDate(val, "yyyy-MM-dd"));
							}
						} else if (elem.equals("writtenDate_" + num)) {
							if (val == null || (val.equals(""))) {
								// p("writtenDate is null");
								rx.setWrittenDate(RxUtil.StringToDate("0000-00-00", "yyyy-MM-dd"));
							} else {
								rx.setWrittenDate(RxUtil.StringToDate(val, "yyyy-MM-dd"));
							}

						} else if (elem.equals("outsideProviderName_" + num)) {
							rx.setOutsideProviderName(val);
						} else if (elem.equals("outsideProviderOhip_" + num)) {
							if (val.equals("") || val == null) {
								rx.setOutsideProviderOhip("0");
							} else {
								rx.setOutsideProviderOhip(val);
							}
						} else if (elem.equals("ocheck_" + num)) {
							if (val.equals("on")) {
								isOutsideProvider = true;
							} else {
								isOutsideProvider = false;
							}
						} else if (elem.equals("pastMed_" + num)) {
							if (val.equals("on")) {
								isPastMed = true;
							} else {
								isPastMed = false;
							}
						} else if (elem.equals("patientComplianceY_" + num)) {
							if (val.equals("on")) {
								patientComplianceY = true;
							} else {
								patientComplianceY = false;
							}
						} else if (elem.equals("patientComplianceN_" + num)) {
							if (val.equals("on")) {
								patientComplianceN = true;
							} else {
								patientComplianceN = false;
							}
						}
					}

					if (!isOutsideProvider) {
						rx.setOutsideProviderName("");
						rx.setOutsideProviderOhip("");
					}
					rx.setPastMed(isPastMed);
					rx.setLongTerm(isLongTerm);
					String newline = System.getProperty("line.separator");
					rx.setPatientCompliance(patientComplianceY, patientComplianceN);
					String special;
					if (rx.isCustomNote()) {
						rx.setQuantity(null);
						rx.setUnitName(null);
						rx.setRepeat(0);
						special = rx.getCustomName() + newline + rx.getSpecial();
						if (rx.getSpecialInstruction() != null && !rx.getSpecialInstruction().equalsIgnoreCase("null") && rx.getSpecialInstruction().trim().length() > 0) special += newline + rx.getSpecialInstruction();
					} else if (rx.isCustom()) {// custom drug
						if (rx.getUnitName() == null) {
							special = rx.getCustomName() + newline + rx.getSpecial();
							if (rx.getSpecialInstruction() != null && !rx.getSpecialInstruction().equalsIgnoreCase("null") && rx.getSpecialInstruction().trim().length() > 0) special += newline + rx.getSpecialInstruction();
							special += newline + "Qty:" + rx.getQuantity() + " Repeats:" + "" + rx.getRepeat();
						} else {
							special = rx.getCustomName() + newline + rx.getSpecial();
							if (rx.getSpecialInstruction() != null && !rx.getSpecialInstruction().equalsIgnoreCase("null") && rx.getSpecialInstruction().trim().length() > 0) special += newline + rx.getSpecialInstruction();
							special += newline + "Qty:" + rx.getQuantity() + " " + rx.getUnitName() + " Repeats:" + "" + rx.getRepeat();
						}
					} else {// non-custom drug
						if (rx.getUnitName() == null) {
							special = rx.getBrandName() + newline + rx.getSpecial();
							if (rx.getSpecialInstruction() != null && !rx.getSpecialInstruction().equalsIgnoreCase("null") && rx.getSpecialInstruction().trim().length() > 0) special += newline + rx.getSpecialInstruction();

							special += newline + "Qty:" + rx.getQuantity() + " Repeats:" + "" + rx.getRepeat();
						} else {
							special = rx.getBrandName() + newline + rx.getSpecial();
							if (rx.getSpecialInstruction() != null && !rx.getSpecialInstruction().equalsIgnoreCase("null") && rx.getSpecialInstruction().trim().length() > 0) special += newline + rx.getSpecialInstruction();
							special += newline + "Qty:" + rx.getQuantity() + " " + rx.getUnitName() + " Repeats:" + "" + rx.getRepeat();
						}
					}

					if (!rx.isCustomNote() && rx.isMitte()) {
						special = special.replace("Qty", "Mitte");
					}

					rx.setSpecial(special.trim());

					bean.addAttributeName(rx.getAtcCode() + "-" + String.valueOf(stashIndex));
					bean.setStashItem(stashIndex, rx);
				}
			} catch (Exception e) {
				logger.error("Error", e);
				continue;
			}
		}
		for (Integer n : existingIndex) {
			if (allIndex.contains(n)) {
				allIndex.remove(n);
			}
		}
		List<Integer> deletedIndex = allIndex;
		// remove closed Rx from stash
		for (Integer n : deletedIndex) {
			bean.removeStashItem(n);
			if (bean.getStashIndex() >= bean.getStashSize()) {
				bean.setStashIndex(bean.getStashSize() - 1);
			}
		}

		saveDrug(request);
		return null;
	}

        public ActionForward getDemoNameAndHIN(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, Exception {
            String demoNo=request.getParameter("demoNo").trim();
            DemographicDao demographicDAO=(DemographicDao)SpringUtils.getBean("demographicDao") ;
            Demographic d=demographicDAO.getDemographic(demoNo);
            HashMap hm=new HashMap();
            if(d!=null){
                hm.put("patientName", d.getDisplayName());
                hm.put("patientHIN", d.getHin());
            }else{
                hm.put("patientName", "Unknown");
                hm.put("patientHIN", "Unknown");
            }
            JSONObject jo=JSONObject.fromObject(hm);
            response.getOutputStream().write(jo.toString().getBytes());
            return null;
        }
	public ActionForward changeToLongTerm(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, Exception {
		String strId = request.getParameter("ltDrugId");
		if (strId != null) {
			int drugId = Integer.parseInt(strId);
			RxSessionBean bean = (RxSessionBean) request.getSession().getAttribute("RxSessionBean");
			if (bean == null) {
				response.sendRedirect("error.html");
				return null;
			}
			RxPrescriptionData rxData = new RxPrescriptionData();
			RxPrescriptionData.Prescription oldRx = rxData.getPrescription(drugId);
			oldRx.setLongTerm(true);
			boolean b = oldRx.Save();
			HashMap hm = new HashMap();
			if (b) hm.put("success", true);
			else hm.put("success", false);
			JSONObject jsonObject = JSONObject.fromObject(hm);
			response.getOutputStream().write(jsonObject.toString().getBytes());
			return null;
		} else {
			HashMap hm = new HashMap();
			hm.put("success", false);
			JSONObject jsonObject = JSONObject.fromObject(hm);
			response.getOutputStream().write(jsonObject.toString().getBytes());
			return null;
		}
	}

	public void saveDrug(final HttpServletRequest request) throws IOException, ServletException, Exception {
		oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) request.getSession().getAttribute("RxSessionBean");

		RxPrescriptionData.Prescription rx = null;
		RxPrescriptionData prescription = new RxPrescriptionData();
		String scriptId = prescription.saveScript(bean);
		StringBuilder auditStr = new StringBuilder();
		ArrayList<String> attrib_names = bean.getAttributeNames();
		for (int i = 0; i < bean.getStashSize(); i++) {
			try {
				rx = bean.getStashItem(i);
				rx.Save(scriptId);// new drug id availble after this line
				bean.addRandomIdDrugIdPair(rx.getRandomId(), rx.getDrugId());
				auditStr.append(rx.getAuditString());
				auditStr.append("\n");
			} catch (Exception e) {
				logger.error("Error", e);
			}
			// Save annotation
			HttpSession se = request.getSession();
			WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(se.getServletContext());
			CaseManagementManager cmm = (CaseManagementManager) ctx.getBean("caseManagementManager");
			String attrib_name = attrib_names.get(i);
			if (attrib_name != null) {
				CaseManagementNote cmn = (CaseManagementNote) se.getAttribute(attrib_name);
				if (cmn != null) {
					cmm.saveNoteSimple(cmn);
					CaseManagementNoteLink cml = new CaseManagementNoteLink();
					cml.setTableName(CaseManagementNoteLink.DRUGS);
					cml.setTableId((long) rx.getDrugId());
					cml.setNoteId(cmn.getId());
					cmm.saveNoteLink(cml);
					se.removeAttribute(attrib_name);
					LogAction.addLog(cmn.getProviderNo(), LogConst.ANNOTATE, CaseManagementNoteLink.DISP_PRESCRIP, scriptId, request.getRemoteAddr(), cmn.getDemographic_no(), cmn.getNote());
				}
			}
			rx = null;
		}

		String ip = request.getRemoteAddr();
		request.setAttribute("scriptId", scriptId);
		LogAction.addLog((String) request.getSession().getAttribute("user"), LogConst.ADD, LogConst.CON_PRESCRIPTION, scriptId, ip, "" + bean.getDemographicNo(), auditStr.toString());

		return;
	}

	public ActionForward checkNoStashItem(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, Exception {
		oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) request.getSession().getAttribute("RxSessionBean");
		int n = bean.getStashSize();
		HashMap hm = new HashMap();
		hm.put("NoStashItem", n);
		JSONObject jsonObject = JSONObject.fromObject(hm);
		response.getOutputStream().write(jsonObject.toString().getBytes());
		return null;
	}
}
<%--

    Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
    This software is published under the GPL GNU General Public License.
    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License
    as published by the Free Software Foundation; either version 2
    of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.

    This software was written for the
    Department of Family Medicine
    McMaster University
    Hamilton
    Ontario, Canada

--%>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic"%>
<%@ page import="oscar.oscarRx.data.*,java.util.*,org.oscarehr.common.dao.DrugReasonDao,org.oscarehr.common.model.DrugReason"%>
<%@page import="org.oscarehr.util.SpringUtils,oscar.util.StringUtils"%>
<%@ page import="org.oscarehr.common.dao.DxresearchDAO,org.oscarehr.common.model.Dxresearch,org.oscarehr.common.dao.Icd9Dao,org.oscarehr.common.model.Icd9" %>
<%@ page import="org.oscarehr.util.MiscUtils" %>
<html:html locale="true">
<head>

<script type="text/javascript" src="<%= request.getContextPath() %>/share/javascript/prototype.js"/>"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/share/javascript/Oscar.js"/>"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/js/global.js"></script>
<title><bean:message key="SelectPharmacy.title" /></title>
<html:base />

<logic:notPresent name="RxSessionBean" scope="session">
	<logic:redirect href="error.html" />
</logic:notPresent>
<logic:present name="RxSessionBean" scope="session">
	<bean:define id="bean" type="oscar.oscarRx.pageUtil.RxSessionBean"
		name="RxSessionBean" scope="session" />
	<logic:equal name="bean" property="valid" value="false">
		<logic:redirect href="error.html" />
	</logic:equal>
</logic:present>

<%
oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean)pageContext.findAttribute("bean");


String demoStr = request.getParameter("demographicNo");
String drugIdStr  = request.getParameter("drugId");
Integer drugId = null;
Integer demoNo = null;

if(drugIdStr !=null){
	drugId = Integer.parseInt(drugIdStr);
}

if(drugId ==null){
	drugId = (Integer) request.getAttribute("drugId");
	drugIdStr = drugId.toString();
}

if(demoStr != null){
	demoNo = Integer.parseInt(demoStr);
}

if(demoNo == null){
	demoNo = (Integer) request.getAttribute("demoNo");
	demoStr = demoNo.toString();
}

DxresearchDAO dxResearchDAO  = (DxresearchDAO) SpringUtils.getBean("dxresearchDAO");
List<Dxresearch> dxList = dxResearchDAO.getByDemographicNo(demoNo);
Icd9Dao icd9Dao = (Icd9Dao)  SpringUtils.getBean("Icd9DAO");

%>

<bean:define id="patient"
	type="oscar.oscarRx.data.RxPatientData.Patient" name="Patient" />
	<style type="text/css">
	body {
		margin:0;
		padding:0;
	}
	label {float:left;width:150px;}
	</style>

<link rel="stylesheet" type="text/css" href="styles.css">
</head>
<body topmargin="0" leftmargin="0" vlink="#0000FF">

<table border="0" cellpadding="0" cellspacing="0" style="border-collapse: collapse" bordercolor="#111111" width="100%" id="AutoNumber1" height="100%">
	<%@ include file="TopLinks.jsp"%><!-- Row One included here-->
	<tr>
		<td valign="top" width="200px;">

        <% for(Dxresearch dx:dxList){
        	String idc9Desc = "N/A";
        	try{
        	   idc9Desc = icd9Dao.getIcd9Code(dx.getDxresearchCode()).get(0).getDescription();
        	}catch(Exception dxException){
        		MiscUtils.getLogger().error("ICD9 Code not found ",dxException );
        	}
        	%>
        		<a href="javascript:void(0);" onclick="$('codeTxt').value='<%=dx.getDxresearchCode()%>'"   title="<%=dx.getDxresearchCode()%> - <%=idc9Desc%>"  ><%=dx.getDxresearchCode()%> - <%=StringUtils.maxLenString(idc9Desc, 10, 6, StringUtils.ELLIPSIS)%></a></br>
        <%}%>

		</td> <!--   Side Bar File --->
		<td width="100%" style="border-left: 2px solid #A9A9A9;" height="100%" valign="top">

		<%if (request.getAttribute("message") !=null){ %>
			<%=request.getAttribute("message") %>
		<%} %>

		<form action="RxReason.do" method="post">
		<fieldset>
		<input type="hidden" name="method" value="addDrugReason"/>
		<input type="hidden" name="demographicNo" value="<%=demoStr%>"   />
        <input type="hidden" name="drugId"  value="<%=drugIdStr%>"  />
		<legend>Add Reason/Protocol for Drug</legend>
		<label>Reason/Protocol</label>
					 	<select name="codingSystem">
							<option value="icd9">icd9</option>
							<%-- option value="limitUse">Limited Use</option --%>
						</select>
		<br>
		<label>Code</label><input type="text" name="code" id="codeTxt"/>
		<br>
		<label>Comments</label> <input type="text" name="comments"/>
		<br>
		<label>Primary Reason For Drug</label> <input type="checkbox" name="primaryReasonFlag" value="true"/>
		<br>
		<input type="submit" value="Save"/>
		</fieldset>
		</form>
		<table cellpadding="0" cellspacing="2" style="border-collapse: collapse" bordercolor="#111111" width="100%" height="100%">

			<!----Start new rows here-->

			<tr>
				<td>

				</td>
			</tr>
			<tr>
				<td>
				<%
				DrugReasonDao drugReasonDao  = (DrugReasonDao) SpringUtils.getBean("drugReasonDao");

				List<DrugReason> drugReasons  = drugReasonDao.getReasonsForDrugID(drugId,true);
				%>


                <div style=" width:500px; height:400px; overflow:auto;">
				<table>
					<tr>
						<th><bean:message key="SelectReason.table.codingSystem" /></th>
						<th><bean:message key="SelectReason.table.code" /></th>
						<th><bean:message key="SelectReason.table.comments" /></th>
						<th><bean:message key="SelectReason.table.primaryReasonFlag" /></th>
						<th><bean:message key="SelectReason.table.provider" /></th>
						<th><bean:message key="SelectReason.table.dateCoded" /><th>
						<th>&nbsp;</th>
					</tr>

					<%for(DrugReason drugReason:drugReasons){ %>
					<tr>
						<td><%=drugReason.getCodingSystem() %></td>
						<td><%=drugReason.getCode() %></td>
						<td><%=drugReason.getComments() %></td>
						<td>
						<%if(drugReason.getPrimaryReasonFlag()){ %>
								True
						<%}%>
						</td>
						<td><%=drugReason.getProviderNo() %></td>
						<td><%=drugReason.getDateCoded() %></td>
						<td>
							<a onclick="$('archive<%=drugReason.getId()%>').toggle();return false;" href="#">archive</a>
						</td>
					</tr>
					<tr id="archive<%=drugReason.getId()%>" style="display:none;">
					    <td colspan="7">
					    <div >

							<form action="RxReason.do" method="post">
								<fieldset>
									<legend>Archive  Coding System: <%=drugReason.getCodingSystem() %> Code: <%=drugReason.getCode() %></legend>
									<input type="hidden" name="method" value="archiveReason"/>
									<input type="hidden" name="reasonId" value="<%=drugReason.getId()%>"/>
 									Reason: <input type="text" name="archiveReason"/>
									<input type="submit" value="Archive"/>
								</fieldset>
							</form>
							</div>
					    </td>
					</tr>
					<%}%>

				</table>
                </div>

				</td>
			</tr>
			<!----End new rows here-->
			<tr height="100%">
				<td></td>
			</tr>
		</table>
		</td>
	</tr>
	<tr>
		<td height="0%" style="border-bottom: 2px solid #A9A9A9; border-top: 2px solid #A9A9A9;"></td>
		<td height="0%" style="border-bottom: 2px solid #A9A9A9; border-top: 2px solid #A9A9A9;"></td>
	</tr>
	<tr>
		<td width="100%" height="0%" colspan="2">&nbsp;</td>
	</tr>
	<tr>
		<td width="100%" height="0%" style="padding: 5" bgcolor="#DCDCDC" colspan="2"></td>
	</tr>
</table>

</body>

</html:html>

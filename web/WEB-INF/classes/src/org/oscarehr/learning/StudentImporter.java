package org.oscarehr.learning;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oscarehr.PMmodule.dao.ProgramDao;
import org.oscarehr.PMmodule.dao.ProviderDao;
import org.oscarehr.PMmodule.dao.SecUserRoleDao;
import org.oscarehr.PMmodule.model.Program;
import org.oscarehr.PMmodule.model.ProgramProvider;
import org.oscarehr.PMmodule.model.SecUserRole;
import org.oscarehr.PMmodule.service.ProgramManager;
import org.oscarehr.common.dao.SecRoleDao;
import org.oscarehr.common.model.Provider;
import org.oscarehr.common.model.SecRole;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.SpringUtils;

import oscar.oscarDB.DBPreparedHandler;

import com.quatro.dao.security.SecurityDao;
import com.quatro.model.security.Security;

public class StudentImporter {

	private static Log logger = LogFactory.getLog(StudentImporter.class);
	
	static ProviderDao providerDao = (ProviderDao)SpringUtils.getBean("providerDao");
	static SecurityDao securityDao = (SecurityDao)SpringUtils.getBean("securityDao");
	static SecRoleDao roleDao = (SecRoleDao)SpringUtils.getBean("secRoleDao");
	static SecUserRoleDao secUserRoleDao = (SecUserRoleDao)SpringUtils.getBean("secUserRoleDao");
	static ProgramDao programDao = (ProgramDao) SpringUtils.getBean("programDao");
	static ProgramManager programManager = (ProgramManager) SpringUtils.getBean("programManager");
	   
	
	public static int importStudentInfo(List<StudentInfo> dataList) {
		
		//create provider record
		DBPreparedHandler dbObj = new DBPreparedHandler();
		
		SecRole studentRole = roleDao.findByName("student");
		if(studentRole == null) {
			logger.warn("Student role not found.");
			return 0;
		}
		SecRole doctorRole = roleDao.findByName("doctor");
		if(doctorRole == null) {
			logger.warn("Doctor role not found.");
			return 0;
		}

		int total = 0;
		String providerNo = null;
		for(StudentInfo data:dataList) {
			try {
				providerNo = dbObj.getNewProviderNo();
				if(providerNo == null || providerNo.length()==0) {
					logger.warn("Failed to generate provider no for student - " + data.getLastName() + "," + data.getFirstName());
					continue;
				}
				
				Provider provider = new Provider();
				provider.setAddress("");
				provider.setBillingNo("");
				provider.setComments("");
				provider.setDob(new Date());
				provider.setFirstName(data.getFirstName());
				provider.setHsoNo("");
				provider.setLastName(data.getLastName());
				provider.setOhipNo("");
				provider.setPhone("");
				provider.setProviderActivity("");
				provider.setProviderNo(providerNo);
				provider.setProviderType("doctor");
				provider.setRmaNo("");
				provider.setSex("M");
				provider.setSignedConfidentiality(new Date());
				provider.setSpecialty("");
				provider.setStatus("1");
				provider.setTeam("");
				provider.setWorkPhone("");
				provider.setPractitionerNo(data.getStudentNumber());
				
				providerDao.saveProvider(provider);
				
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.YEAR, 4);
				
				//save a login record
				Security secRecord = new Security();
				secRecord.setBExpireset(1);
				secRecord.setBLocallockset(1);
				secRecord.setBRemotelockset(0);
				secRecord.setDateExpiredate(cal.getTime());
				secRecord.setPassword(data.getPassword());
				secRecord.setPin(data.getPin());
				secRecord.setProviderNo(providerNo);
				secRecord.setUserName(data.getUsername());
				
				securityDao.save(secRecord);
				
				//assign student role			
				SecUserRole userRole = new SecUserRole();
				userRole.setProviderNo(providerNo);
				userRole.setRoleName(studentRole.getName());	
				userRole.setActive(true);
				secUserRoleDao.save(userRole);
				
				//assign student role			
				SecUserRole drRole = new SecUserRole();
				drRole.setProviderNo(providerNo);
				drRole.setRoleName(doctorRole.getName());	
				drRole.setActive(true);
				secUserRoleDao.save(drRole);
				
				
				//create student's "personal" program
				int facilityId = LoggedInInfo.loggedInInfo.get().currentFacility.getId();
				Program p = new Program();
				try {					
					p.setName("program"+providerNo);
					p.setMaxAllowed(999);
					p.setFacilityId(facilityId);
					p.setSiteSpecificField("student");
					p.setAddress("");
					p.setAbstinenceSupport("");
					p.setDescription("program"+providerNo);
					p.setEmail("");
					p.setEmergencyNumber("");
					p.setExclusiveView("");
					p.setFacilityDesc("");
					p.setFax("");
					p.setLocation("");
					p.setPhone("");
					p.setType("service");
					p.setUrl("");
					programDao.saveProgram(p);
				}catch(Exception e) {
					logger.error(e);					
				}		
				
				//logger.info("need to assign provider to program: " + p.getId());
				
				//assign student to his/her program
				ProgramProvider pp = new ProgramProvider();
				
				pp.setProgramId(new Long(p.getId()));
				pp.setProviderNo(providerNo);
				pp.setRoleId(new Long(studentRole.getId()));
				programManager.saveProgramProvider(pp);
				
				total ++ ;
			}catch(Exception e) {
				logger.error(e);
			}
			
		}
		return total;
	}
}
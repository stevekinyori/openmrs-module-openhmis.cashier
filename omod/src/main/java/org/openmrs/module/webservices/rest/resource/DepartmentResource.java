/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.webservices.rest.resource;

import org.openmrs.module.openhmis.cashier.api.IDepartmentService;
import org.openmrs.module.openhmis.cashier.api.model.Department;
import org.openmrs.module.openhmis.commons.api.entity.IMetadataDataService;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;

@Resource(name=RestConstants.VERSION_2 + "/cashier/department", supportedClass=Department.class, supportedOpenmrsVersions={"1.9"})
public class DepartmentResource extends BaseRestMetadataResource<Department> {

	@Override
	public Department newDelegate() {
		return new Department();
	}

	@Override
	public Class<? extends IMetadataDataService<Department>> getServiceClass() {
		return IDepartmentService.class;
	}
}



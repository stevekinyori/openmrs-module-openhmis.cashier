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

package org.openmrs.module.openhmis.cashier.api.model;

import org.openmrs.BaseOpenmrsMetadata;

public class ItemCode extends BaseOpenmrsMetadata {
	private Integer itemCodeId;
	private String code;
	private Item item;

	public ItemCode() {
		super();
	}

	public ItemCode(String code, String name) {
		super();

		this.code = code;
		setName(name);
	}

	@Override
	public Integer getId() {
		return itemCodeId;
	}

	@Override
	public void setId(Integer id) {
		itemCodeId = id;
	}

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}

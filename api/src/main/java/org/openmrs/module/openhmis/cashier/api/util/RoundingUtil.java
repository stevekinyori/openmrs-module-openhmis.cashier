/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and
 * limitations under the License.
 *
 * Copyright (C) OpenHMIS.  All Rights Reserved.
 */
package org.openmrs.module.openhmis.cashier.api.util;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.logging.Log;
import org.openmrs.GlobalProperty;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.openhmis.cashier.ModuleSettings;
import org.openmrs.module.openhmis.cashier.api.ICashierOptionsService;
import org.openmrs.module.openhmis.cashier.api.model.Bill;
import org.openmrs.module.openhmis.cashier.api.model.BillLineItem;
import org.openmrs.module.openhmis.cashier.api.model.CashierOptions;
import org.openmrs.module.openhmis.inventory.api.IDepartmentDataService;
import org.openmrs.module.openhmis.inventory.api.IItemDataService;
import org.openmrs.module.openhmis.inventory.api.model.Department;
import org.openmrs.module.openhmis.inventory.api.model.Item;
import org.openmrs.module.openhmis.inventory.api.model.ItemPrice;

public class RoundingUtil {
	protected RoundingUtil() {}
	
	public static BigDecimal round(BigDecimal value, BigDecimal nearest, CashierOptions.RoundingMode mode) {
		if (nearest.equals(BigDecimal.ZERO)) {
			return value;
		}
		BigDecimal factor = BigDecimal.ONE.divide(nearest);
		int scale = nearest.scale();
		switch (mode) {
			case FLOOR:
				return value.multiply(factor).setScale(value.scale(), BigDecimal.ROUND_FLOOR).divide(factor)
				        .setScale(scale, BigDecimal.ROUND_FLOOR);
			case CEILING:
				return value.multiply(factor).setScale(value.scale(), BigDecimal.ROUND_CEILING).divide(factor)
				        .setScale(scale, BigDecimal.ROUND_CEILING);
			default:
				return value.multiply(factor).setScale(value.scale(), BigDecimal.ROUND_HALF_UP).divide(factor)
				        .setScale(scale, BigDecimal.ROUND_HALF_UP);
		}
	}
	
	public static void setupRoundingDeptAndItem(Log log) {
		/*
		 * Automatically add rounding item & department
		 */
		AdministrationService adminService = Context.getService(AdministrationService.class);
		
		String nearest = adminService.getGlobalProperty(ModuleSettings.ROUND_TO_NEAREST_PROPERTY);
		if (nearest != null && !nearest.isEmpty() && !nearest.equals("0")) {
			MessageSourceService msgService = Context.getMessageSourceService();
			IDepartmentDataService deptService = Context.getService(IDepartmentDataService.class);
			IItemDataService itemService = Context.getService(IItemDataService.class);
			
			Integer itemId;
			Integer deptId;
			
			deptId = parseDepartmentId(adminService);
			itemId = parseItemId(adminService);
			
			if (deptId == null && itemId == null) {
				Department department = new Department();
				String name = msgService.getMessage("openhmis.cashier.rounding.itemName");
				String description = msgService.getMessage("openhmis.cashier.rounding.itemDescription");
				department.setName(name);
				department.setDescription(description);
				department.setRetired(true);
				department.setRetireReason("Used by Cashier Module for rounding adjustments.");
				deptService.save(department);
				log.info("Created department for rounding item (ID = " + department.getId() + ")...");
				adminService.saveGlobalProperty(new GlobalProperty(ModuleSettings.ROUNDING_DEPT_ID, department.getId()
				        .toString()));
				
				Item item = new Item();
				item.setName(name);
				item.setDescription(description);
				item.setDepartment(department);
				
				ItemPrice price = item.addPrice(name, BigDecimal.ZERO);
				item.setDefaultPrice(price);
				itemService.save(item);
				log.info("Created item for rounding (ID = " + item.getId() + ")...");
				adminService
					.saveGlobalProperty(new GlobalProperty(ModuleSettings.ROUNDING_ITEM_ID, item.getId().toString()));
			}
		}
	}
	
	/**
	 * Add a rounding line item to a bill if necessary
	 * @param bill
	 * @should handle a rounding line item (add/update/delete with the appropriate value)
	 * @should not modify a bill that needs no rounding
	 */
	public static void handleRoundingLineItem(Bill bill) {
		ICashierOptionsService cashOptService = Context.getService(ICashierOptionsService.class);
		CashierOptions options = cashOptService.getOptions();
		if (options.getRoundToNearest().equals(BigDecimal.ZERO)) {
			return;
		}
		
		if (options.getRoundingItemUuid() == null) {
			throw new APIException(
					"No rounding item specified in options. This must be set in order to use rounding for bill totals.");
		}
		
		// Get rounding item
		IItemDataService itemService = Context.getService(IItemDataService.class);
		Item roundingItem = itemService.getByUuid(options.getRoundingItemUuid());
		
		BillLineItem roundingLineItem = findRoundingLineItem(bill, roundingItem);
		
		BigDecimal difference = calculateRoundingValue(bill, options, roundingLineItem);
		
		if (difference.equals(BigDecimal.ZERO) && roundingLineItem != null ) {
			bill.removeLineItem(roundingLineItem);
		} else  if (!difference.equals(BigDecimal.ZERO) && roundingLineItem == null ) {
			// Create line item for rounding item and the required amount
			bill.addLineItem(roundingItem, difference.abs(), "", difference.compareTo(BigDecimal.ZERO) > 0 ? -1 : 1);
		} else  if(!difference.equals(BigDecimal.ZERO)){
			updateRoundingItem(bill, difference, roundingLineItem);
		}
		bill.recalculateLineItemOrder();
	}
	
	private static BillLineItem findRoundingLineItem(Bill bill, Item roundingItem) {
		BillLineItem result = null;
		for (BillLineItem lineItem : bill.getLineItems()) {
			if (roundingItem.equals(lineItem.getItem())) {
			result = lineItem;
				break;
			}
		}
		return result;
	}
	
	private static void updateRoundingItem(Bill bill, BigDecimal difference, BillLineItem roundingLineItem) {
		roundingLineItem.setPrice(difference.abs());
		roundingLineItem.setQuantity(difference.compareTo(BigDecimal.ZERO) > 0 ? -1 : 1);
		
		bill.removeLineItem(roundingLineItem);
		bill.addLineItem(roundingLineItem);
	}

	private static BigDecimal calculateRoundingValue(Bill bill, CashierOptions options, BillLineItem roundingLineItem) {
		List<BillLineItem> lineItems = bill.getLineItems();
		BigDecimal itemTotal = new BigDecimal(0);
		
		if (lineItems == null) {
			return BigDecimal.ZERO;
		}
		
		for (BillLineItem lineItem : lineItems) {
			if (lineItem != null && !lineItem.getVoided()) {
				if (roundingLineItem == null || !roundingLineItem.equals(lineItem)) {
					itemTotal = itemTotal.add(lineItem.getTotal());
				}
			}
		}
		
		return itemTotal.subtract(RoundingUtil.round(itemTotal, options.getRoundToNearest(), options.getRoundingMode()));
				
	}

	private static Integer parseItemId(AdministrationService adminService) {
		Integer itemId;
		try {
			itemId = Integer.parseInt(adminService.getGlobalProperty(ModuleSettings.ROUNDING_ITEM_ID));
		} catch (NumberFormatException e) {
			itemId = null;
		}
		return itemId;
	}
	
	private static Integer parseDepartmentId(AdministrationService adminService) {
		Integer deptId;
		try {
			deptId = Integer.parseInt(adminService.getGlobalProperty(ModuleSettings.ROUNDING_DEPT_ID));
		} catch (NumberFormatException e) {
			deptId = null;
		}
		return deptId;
	}
}

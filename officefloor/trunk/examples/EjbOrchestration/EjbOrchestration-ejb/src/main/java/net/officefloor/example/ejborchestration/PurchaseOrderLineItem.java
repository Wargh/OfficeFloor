/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2010 Daniel Sagenschneider
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.officefloor.example.ejborchestration;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Line item {@link Entity} within a {@link PurchaseOrder}.
 * 
 * @author Daniel Sagenschneider
 */
@Entity
public class PurchaseOrderLineItem {

	/**
	 * Line item Id.
	 */
	@Id
	@GeneratedValue
	private Long lineItemId;

	/**
	 * {@link Product} for this {@link PurchaseOrderLineItem}.
	 */
	@ManyToOne
	private Product product;

	/**
	 * Quantity of the {@link Product}.
	 */
	private int quantity;

	/**
	 * Default constructor for the {@link Entity}.
	 */
	public PurchaseOrderLineItem() {
	}

	/**
	 * Initiate.
	 * 
	 * @param product
	 *            {@link Product}.
	 * @param quantity
	 *            Quantity of the {@link Product}.
	 */
	public PurchaseOrderLineItem(Product product, int quantity) {
		this.product = product;
		this.quantity = quantity;
	}

	/**
	 * Obtains the identifier for this {@link PurchaseOrderLineItem}.
	 * 
	 * @return Identifier for this {@link PurchaseOrderLineItem}.
	 */
	public Long getPurchaseOrderLineItemId() {
		return this.lineItemId;
	}

	/**
	 * Obtains the {@link Product}.
	 * 
	 * @return {@link Product}.
	 */
	public Product getProduct() {
		return this.product;
	}

	/**
	 * Obtains the quantity.
	 * 
	 * @return Quantity.
	 */
	public int getQuantity() {
		return this.quantity;
	}

	/**
	 * Specifies the quantity.
	 * 
	 * @param quantity
	 *            Quantity.
	 */
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

}
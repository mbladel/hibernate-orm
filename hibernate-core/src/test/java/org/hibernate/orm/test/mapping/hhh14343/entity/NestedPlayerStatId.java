/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.hhh14343.entity;

import java.io.Serializable;

public class NestedPlayerStatId implements Serializable
{
	private static final long serialVersionUID = 1L;

	private Integer playerId;

	private NestedScoreId score;

	public NestedPlayerStatId()
	{
	}

	public Integer getPlayerId()
	{
		return playerId;
	}

	public void setPlayerId(Integer playerId)
	{
		this.playerId = playerId;
	}

	public NestedScoreId getScore()
	{
		return score;
	}

	public void setScore(NestedScoreId score)
	{
		this.score = score;
	}
}

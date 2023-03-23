/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.batch;

import java.io.Serializable;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Generated;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.generator.EventType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@SessionFactory
@DomainModel( annotatedClasses = {
		BatchForeignKeyInsertOrderTest.Interpretation.class,
		BatchForeignKeyInsertOrderTest.InterpretationData.class,
		BatchForeignKeyInsertOrderTest.InterpretationVersion.class
} )
@ServiceRegistry( settings = @Setting( name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "10" ) )
public class BatchForeignKeyInsertOrderTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Long interpretationVersion = 111L;

			final Interpretation interpretation = new Interpretation();
			interpretation.uuid = UUID.randomUUID();

			final InterpretationData interpretationData = new InterpretationData();
			interpretationData.interpretationVersion = new InterpretationVersion(
					interpretationVersion,
					interpretation.uuid
			);
			interpretationData.name = "TEST_NAME";
			session.persist( interpretationData );

			interpretation.interpretationData = interpretationData;
			interpretation.interpretationVersion = interpretationVersion;
			session.persist( interpretation );
		} );
	}

	@Entity
	@Table( name = "interpretations", uniqueConstraints = @UniqueConstraint( name = "interpretations_id_unique", columnNames = {
			"id"
	} ) )
	public static class Interpretation {
		@Id
		public UUID uuid;

		@Column( name = "interpretation_version" )
		public Long interpretationVersion;

		@Column( name = "id", insertable = false, updatable = false, nullable = false, columnDefinition = "serial" )
		@Generated // FIXME @Generated disables batching, but in cases like this (having FKs) it might be a problem
		public Long id;

		@OneToOne( fetch = FetchType.LAZY )
		@JoinColumns( {
				@JoinColumn( name = "uuid", referencedColumnName = "interpretation_uuid", insertable = false, updatable = false ),
				@JoinColumn( name = "interpretation_version", referencedColumnName = "interpretation_version", insertable = false, updatable = false )
		} )
		public InterpretationData interpretationData;
	}

	@Entity
	@Table( name = "interpretation_data" )

	public static class InterpretationData {
		@EmbeddedId
		public InterpretationVersion interpretationVersion;

		@Column( updatable = false )
		public String name;
	}

	@Embeddable
	public static class InterpretationVersion implements Serializable {
		@Column( name = "interpretation_version", nullable = false, updatable = false )
		public Long version;

		@Column( name = "interpretation_uuid", nullable = false, updatable = false )
		public UUID uuid;

		public InterpretationVersion() {
		}

		public InterpretationVersion(Long version, UUID uuid) {
			this.version = version;
			this.uuid = uuid;
		}
	}
}

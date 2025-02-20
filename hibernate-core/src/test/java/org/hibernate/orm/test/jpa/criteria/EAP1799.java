package org.hibernate.orm.test.jpa.criteria;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Jpa(annotatedClasses = {
		EAP1799.EntIngotIdPK.class,
		EAP1799.EntIngotId.class,
		EAP1799.EntIngotRelationship.class,
		EAP1799.EntIngotNoRelationship.class,
})
public class EAP1799 {
	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final Date now = new Date();

			final EntIngotNoRelationship entNo = new EntIngotNoRelationship();
			entNo.ingotKey = 1L;
			entNo.numberSerial = "1";
			entNo.codeIngotToken = now;
			em.persist( entNo );

			final EntIngotId id1 = new EntIngotId();
			id1.entIngot = entNo;
			id1.id = new EntIngotIdPK("1", now);
			em.persist( id1 );

			final EntIngotId id2 = new EntIngotId();
			id2.entIngot = entNo;
			id2.id = new EntIngotIdPK("2", now);
			em.persist( id2 );
		} );
	}

	@Test
	public void test(EntityManagerFactoryScope scope) {
		final EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		final EntIngotRelationship result = EntIngotRelationship.findBySerial( em, "1" );
		assert result != null;
		em.close();
	}

	@Embeddable
	static class EntIngotIdPK implements Serializable {
		// default serial version id, required for serializable classes.
		private static final long serialVersionUID = 1L;

		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "CODE_INGOT_TOKEN")
		private Date codeIngotToken;

		@Column(name = "NUMBER_SERIAL")
		private String numberSerial;

		public EntIngotIdPK() {
		}

		public EntIngotIdPK(String numberSerial, Date codeIngotToken) {
			this.numberSerial = numberSerial;
			this.codeIngotToken = codeIngotToken;
		}

		public String getNumberSerial() {
			return this.numberSerial == null ? null : this.numberSerial.trim();
		}

		public void setNumberSerial(String numberSerial) {
			this.numberSerial = numberSerial;
		}
	}

	@Entity(name = "EntIngotId")
	static class EntIngotId implements Serializable {
		private static final long serialVersionUID = 1L;

		@EmbeddedId
		private EntIngotIdPK id;

		@ManyToOne
		@JoinColumn(name = "CODE_INGOT_TOKEN", referencedColumnName = "CODE_INGOT_TOKEN", insertable = false, updatable = false)
		private EntIngotNoRelationship entIngot;

		public EntIngotId() {
		}

		public EntIngotIdPK getId() {
			return this.id;
		}

		public void setId(EntIngotIdPK id) {
			this.id = id;
		}

		public EntIngotNoRelationship getEntIngot() {
			return this.entIngot;
		}

		public void setEntIngot(EntIngotNoRelationship entIngot) {
			this.entIngot = entIngot;
		}

	}

	@Entity(name = "EntIngotRelationship")
	@Table(name = "EntIngot")
	@NamedQueries({
			@NamedQuery(name = "EntIngotRelationship.findBySerial", query = "select distinct object(i) from EntIngotRelationship i, IN (i.entIdList) as ingotIdList where ingotIdList.id.numberSerial = ?1"),
	})
	static class EntIngotRelationship implements Serializable {
		private static final long serialVersionUID = 1L;

		public static EntIngotRelationship findBySerial(EntityManager em, String serial) {
			if ( serial == null ) {
				return null;
			}

			serial = serial.toUpperCase();
			Query qry = em.createNamedQuery( "EntIngotRelationship.findBySerial" );
			qry.setParameter( 1, serial );
			try {
				return (EntIngotRelationship) qry.getSingleResult();
			}
			catch (Exception e) {
				return null;
			}
		}

		@Id
		@Column(name = "INGOT_KEY")
		private long ingotKey;

		@Column(name = "NUMBER_SERIAL")
		private String numberSerial;

		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "CODE_INGOT_TOKEN")
		private Date codeIngotToken;

		@OneToMany(mappedBy = "entIngot", fetch = FetchType.EAGER)
		private Set<EntIngotId> entIdList;

		public EntIngotRelationship() {
		}

		public long getIngotKey() {
			return ingotKey;
		}

		public void setIngotKey(long ingotKey) {
			this.ingotKey = ingotKey;
		}

		public String getNumberSerial() {
			return this.numberSerial == null ? null : this.numberSerial.trim();
		}

		public void setNumberSerial(String numberSerial) {
			this.numberSerial = numberSerial;
		}

		public Set<EntIngotId> getEntIdList() {
			return this.entIdList;
		}

		public void setEntIdList(Set<EntIngotId> entIdList) {
			this.entIdList = entIdList;
		}

		public Date getCodeIngotToken() {
			return codeIngotToken;
		}

		public void setCodeIngotToken(Date codeIngotToken) {
			this.codeIngotToken = codeIngotToken;
		}
	}

	@Entity(name = "EntIngotNoRelationship")
	@Table(name = "EntIngot")
	@NamedQueries({
			@NamedQuery(name = "EntIngotNoRelationship.findBySerial", query = "select distinct object(i) from EntIngotNoRelationship i where i.numberSerial = ?1"),
	})
	static class EntIngotNoRelationship implements Serializable {
		private static final long serialVersionUID = 1L;

		public static EntIngotNoRelationship findBySerial(EntityManager em, String serial) {
			if ( serial == null ) {
				return null;
			}

			serial = serial.toUpperCase();
			Query qry = em.createNamedQuery( "EntIngotNoRelationship.findBySerial" );
			qry.setParameter( 1, serial );
			try {
				return (EntIngotNoRelationship) qry.getSingleResult();
			}
			catch (Exception e) {
				return null;
			}
		}

		@Id
		@Column(name = "INGOT_KEY")
		private long ingotKey;

		@Column(name = "NUMBER_SERIAL")
		private String numberSerial;

		@Column(name = "CODE_INGOT_TOKEN")
		private Date codeIngotToken;

		public EntIngotNoRelationship() {
		}

		public long getIngotKey() {
			return ingotKey;
		}

		public void setIngotKey(long ingotKey) {
			this.ingotKey = ingotKey;
		}

		public String getNumberSerial() {
			return this.numberSerial == null ? null : this.numberSerial.trim();
		}

		public void setNumberSerial(String numberSerial) {
			this.numberSerial = numberSerial;
		}

		public Date getCodeIngotToken() {
			return codeIngotToken;
		}

		public void setCodeIngotToken(Date codeIngotToken) {
			this.codeIngotToken = codeIngotToken;
		}
	}
}

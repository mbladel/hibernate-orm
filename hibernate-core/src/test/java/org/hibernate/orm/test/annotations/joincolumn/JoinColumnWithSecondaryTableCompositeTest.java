/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.joincolumn;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		JoinColumnWithSecondaryTableCompositeTest.Description.class,
		JoinColumnWithSecondaryTableCompositeTest.Country.class,
		JoinColumnWithSecondaryTableCompositeTest.CountryComposite.class
})
public class JoinColumnWithSecondaryTableCompositeTest {

	@Test
	public void test(SessionFactoryScope scope) {

	}

	@Entity(name = "Description")
	@Table(name = "description_table")
	public static class Description implements Serializable {
		@Id
		@Column(name = "description_language_number", length = 11, updatable = false, nullable = false)
		private Long sequenceLanguageNumber;

		@Id
		@Column(name = "translation_locale", length = 5, updatable = false, nullable = false)
		private Locale translationLocale;

		@Column(name = "value", length = 512, nullable = false, unique = false)
		private String value;


		public Long getSequenceLanguageNumber() {
			return sequenceLanguageNumber;
		}

		public void setSequenceLanguageNumber(Long sequenceLanguageNumber) {
			this.sequenceLanguageNumber = sequenceLanguageNumber;
		}

		public Locale getTranslationLocale() {
			return translationLocale;
		}

		public void setTranslationLocale(Locale translationLocale) {
			this.translationLocale = translationLocale;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	@Entity(name = "Country")
	@Table(name = "country_table")
	@SecondaryTable(name = "country_secondary", pkJoinColumns = {
			@PrimaryKeyJoinColumn(name = "country_secondary_iso", referencedColumnName = "country_iso")
	})
	public static class Country implements Serializable {
		@Column(name = "country_composite_business", length = 2, updatable = false, nullable = false)
		private Integer business;

		@Id
		@Column(name = "country_iso", length = 3, nullable = false, unique = false)
		private String isoCountryAlpha2Code;

		@Column(name = "country_sequence_number", length = 11, table = "country_secondary")
		private Long sequenceLanguageNumber;

		@OneToMany(fetch = FetchType.EAGER)
		@JoinColumn(name = "description_language_number", referencedColumnName = "country_sequence_number")
		private List<Description> descriptions;


		public String getIsoCountryAlpha2Code() {
			return isoCountryAlpha2Code;
		}

		public void setIsoCountryAlpha2Code(String isoCountryAlpha2Code) {
			this.isoCountryAlpha2Code = isoCountryAlpha2Code;
		}

		public Long getSequenceLanguageNumber() {
			return sequenceLanguageNumber;
		}

		public void setSequenceLanguageNumber(Long sequenceLanguageNumber) {
			this.sequenceLanguageNumber = sequenceLanguageNumber;
		}

		public List<Description> getDescriptions() {
			return descriptions;
		}

		public void setDescriptions(List<Description> descriptions) {
			this.descriptions = descriptions;
		}
	}


	public static class CountryId implements Serializable {
		private Integer business;

		private String isoCountryAlpha2Code;

		public Integer getBusiness() {
			return business;
		}

		public void setBusiness(Integer business) {
			this.business = business;
		}

		public String getIsoCountryAlpha2Code() {
			return isoCountryAlpha2Code;
		}

		public void setIsoCountryAlpha2Code(String isoCountryAlpha2Code) {
			this.isoCountryAlpha2Code = isoCountryAlpha2Code;
		}
	}


	@Entity(name = "CountryComposite")
	@Table(name = "country_composite_table")
	@IdClass(CountryId.class)
	@SecondaryTable(name = "country_composite_secondary", pkJoinColumns = {
			@PrimaryKeyJoinColumn(name = "country_secondary_business", referencedColumnName = "country_composite_business"),
			@PrimaryKeyJoinColumn(name = "country_secondary_iso", referencedColumnName = "country_composite_iso")
	})
	public static class CountryComposite implements Serializable {
		@Id
		@Column(name = "country_composite_business", length = 2, updatable = false, nullable = false)
		private Integer business;

		@Id
		@Column(name = "country_composite_iso", length = 3, nullable = false, unique = false)
		private String isoCountryAlpha2Code;

		@Column(name = "country_sequence_number", length = 11, table = "country_composite_secondary")
		private Long sequenceLanguageNumber;

		@Column(name = "country_another_number", length = 11, table = "country_composite_secondary")
		private Long anotherNumber;

		@OneToMany(fetch = FetchType.EAGER)
		// nb: commenting _business and uncommenting this column works bc on the same table,
		// even if it's in the secondary with composite key
//		@JoinColumn(name = "description_another_number", referencedColumnName = "country_another_number")
		@JoinColumn(name = "description_business", referencedColumnName = "country_composite_business")
		@JoinColumn(name = "description_language_number", referencedColumnName = "country_sequence_number")
		private List<Description> descriptions;


		public String getIsoCountryAlpha2Code() {
			return isoCountryAlpha2Code;
		}

		public void setIsoCountryAlpha2Code(String isoCountryAlpha2Code) {
			this.isoCountryAlpha2Code = isoCountryAlpha2Code;
		}

		public Long getSequenceLanguageNumber() {
			return sequenceLanguageNumber;
		}

		public void setSequenceLanguageNumber(Long sequenceLanguageNumber) {
			this.sequenceLanguageNumber = sequenceLanguageNumber;
		}

		public List<Description> getDescriptions() {
			return descriptions;
		}

		public void setDescriptions(List<Description> descriptions) {
			this.descriptions = descriptions;
		}
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.generator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.io.Serializable;
import java.util.EnumSet;

import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;

/**
 * Describes the generation of values of a certain field or property of an entity. A generated
 * value might be generated in Java, or by the database. Every instance of this interface can implement
 * {@link BeforeExecutionGenerator} or {@link OnExecutionGenerator} depending on whether values
 * are generated in Java code before execution of a SQL statement, or by the database when the
 * SQL statement is executed.
 * <ul>
 * <li>Value generation via arbitrary code written in Java is the responsibility of the method
 *     {@link BeforeExecutionGenerator#generate(SharedSessionContractImplementor, Object, Object, EventType)}.
 *	   In this case, the generated value is written to the database just like any other field
 *	   or property value. The Java code may, of course, ask the database to actually generate
 *	   the value. Examples include timestamp generation using the JVM system time, and id
 *	   generation using a database sequence.
 * <li>A value generated by the database might be generated implicitly, by a trigger, or using
 *	   a {@code default} column value specified in DDL, for example, or it might be generated
 *	   by a SQL expression occurring explicitly in the SQL {@code insert} or {@code update}
 *	   statement. In this case, the generated value may be retrieved from the database using a
 *	   SQL {@code select}, though in certain cases this additional round trip may be avoided.
 *	   An important example is id generation using an identity column.
 * </ul>
 * Note that it's also possible for a Generator to implement both interfaces and
 * determine the timing of ID generation at runtime based on a custom condition,
 * see {@link #generatedOnExecution(SharedSessionContractImplementor, Object) generatedOnExecution}.
 * <p>
 * Generically, a generator may be integrated with the program using the meta-annotation
 * {@link org.hibernate.annotations.ValueGenerationType}, which associates the generator with
 * a Java annotation type, called the <em>generator annotation</em>. A generator may receive
 * parameters from its generator annotation. The generator may either:
 * <ul>
 * <li>implement {@link AnnotationBasedGenerator}, and receive the annotation as an argument to
 *     {@link AnnotationBasedGenerator#initialize},
 * <li>declare a constructor with the same signature as {@link AnnotationBasedGenerator#initialize},
 * <li>declare a constructor which accepts just the annotation instance, or
 * <li>declare a only default constructor, in which case it will not receive parameters.
 * </ul>
 * <p>
 * A generator must implement {@link #getEventTypes()} to specify the events for which it should be
 * called to produce a new value. {@link EventTypeSets} provides a convenient list of possibilities.
 * <p>
 * There are two especially important applications of this machinery:
 * <ul>
 * <li>
 * An {@linkplain jakarta.persistence.Id identifier} generator is a generator capable of producing
 * surrogate primary key values. An identifier generator must respond to insert events only. That
 * is, {@link #getEventTypes()} must return {@link EventTypeSets#INSERT_ONLY}. It may be integrated
 * using the {@link org.hibernate.annotations.IdGeneratorType} meta-annotation or the older-style
 * {@link org.hibernate.annotations.GenericGenerator} annotation.
 * <li>
 * A {@linkplain jakarta.persistence.Version version} generator is a generator capable of seeding
 * and incrementing version numbers. A version generator must respond to both insert and update
 * events. That is, {@link #getEventTypes()} must return {@link EventTypeSets#INSERT_AND_UPDATE}.
 * It may be integrated using {@link org.hibernate.annotations.ValueGenerationType} meta-annotation.
 * </ul>
 *
 * @see org.hibernate.annotations.ValueGenerationType
 * @see org.hibernate.annotations.IdGeneratorType
 * @see org.hibernate.annotations.Generated
 *
 * @author Steve Ebersole
 * @author Gavin King
 *
 * @since 6.2
 */
public interface Generator extends Serializable {
	/**
	 * Determines if the property value is generated when a row is written to the database,
	 * or in Java code that executes before the row is written.
	 * <ul>
	 * <li>Generators which only implement {@link BeforeExecutionGenerator} must result
	 *     {@code false}.
	 * <li>Generators which only implement {@link OnExecutionGenerator} must result
	 *     {@code true}.
	 * <li>Generators which implement both subinterfaces may decide at runtime what value
	 *     to return.
	 * </ul>
	 *
	 * @return {@code true} if the value is generated by the database as a side effect of
	 *         the execution of an {@code insert} or {@code update} statement, or false if
	 *         it is generated in Java code before the statement is executed via JDBC.
	 */
	boolean generatedOnExecution();


	/**
	 * Determines if the property value is generated when a row is written to the database,
	 * or in Java code that executes before the row is written.
	 * <p>
	 * Defaults to {@link #generatedOnExecution()}, but can be overloaded allowing conditional
	 * in-database / before execution value generation based on the current state of the owner object.
	 * Note that a generator <b>must</b> implement both {@link BeforeExecutionGenerator} and
	 * {@link OnExecutionGenerator} to achieve this behavior.
	 * method should be overridden to return {@code true}.
	 *
	 * @param session The session from which the request originates.
	 * @param owner The instance of the object owning the attribute for which we are generating a value.
	 *
	 * @return {@code true} if the value is generated by the database as a side effect of
	 * the execution of an {@code insert} or {@code update} statement, or false if
	 * it is generated in Java code before the statement is executed via JDBC.
	 *
	 * @see #generatedOnExecution()
	 * @see BeforeExecutionGenerator
	 * @see OnExecutionGenerator
	 * @since 6.4
	 */
	default boolean generatedOnExecution(SharedSessionContractImplementor session, Object owner) {
		return generatedOnExecution();
	}

	/**
	 * The {@linkplain EventType event types} for which this generator should be called
	 * to produce a new value.
	 * <p>
	 * Identifier generators must return {@link EventTypeSets#INSERT_ONLY}.
	 *
	 * @return a set of {@link EventType}s.
	 */
	EnumSet<EventType> getEventTypes();

	default boolean generatesSometimes() {
		return !getEventTypes().isEmpty();
	}

	default boolean generatesOnInsert() {
		return getEventTypes().contains(INSERT);
	}

	default boolean generatesOnUpdate() {
		return getEventTypes().contains(UPDATE);
	}
}

// Copyright (c) 2006 - 2015, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package com.complexible.pellet.client.reasoner;

import java.util.Set;

import com.clarkparsia.modularity.IncrementalClassifier;
import com.clarkparsia.owlapi.explanation.GlassBoxExplanation;
import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;
import com.clarkparsia.owlapi.explanation.MultipleExplanationGenerator;
import com.clarkparsia.owlapi.explanation.SatisfiabilityConverter;
import com.clarkparsia.owlapiv3.OntologyUtils;
import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerConfiguration;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLLogicalEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.NodeSet;

/**
 * @author Evren Sirin
 */
public class LocalSchemaReasoner implements SchemaReasoner {
	private OWLOntologyManager manager;

	private IncrementalClassifier reasoner;

	private SatisfiabilityConverter converter;

	private GlassBoxExplanation singleExpGen;

	private MultipleExplanationGenerator multipleExpGen;

	public LocalSchemaReasoner(final OWLOntology ont) {
		PelletReasoner pellet = new PelletReasoner(ont, new PelletReasonerConfiguration());

		manager = pellet.getRootOntology().getOWLOntologyManager();

		reasoner = new IncrementalClassifier(pellet);
		// explanation generator makes changes to the ontology that would cause us to lose the current state
		// so we disable change tracking
		manager.removeOntologyChangeListener(reasoner);

		converter = new SatisfiabilityConverter(manager.getOWLDataFactory());

		singleExpGen = new GlassBoxExplanation(new PelletReasonerFactory(), pellet);
		multipleExpGen = new HSTExplanationGenerator(singleExpGen);
	}


	@Override
	public <T extends OWLObject> NodeSet<T> query(final SchemaQuery query, final OWLLogicalEntity input) {
		return SchemaReasonerUtil.query(reasoner, query, input);
	}


	@Override
	public Set<Set<OWLAxiom>> explain(OWLAxiom axiom, int limit) {
		OWLClassExpression ce = converter.convert(axiom);
		return multipleExpGen.getExplanations(ce, 0);
	}


	@Override
	public void update(Set<OWLAxiom> additions, Set<OWLAxiom> removals) {
		manager.addOntologyChangeListener(reasoner);
		OWLOntology ont = reasoner.getRootOntology();
		OntologyUtils.addAxioms(ont, additions);
		OntologyUtils.removeAxioms(ont, removals);
		manager.removeOntologyChangeListener(reasoner);
	}

	@Override
	public void close() throws Exception {
		multipleExpGen.dispose();
		reasoner.dispose();
	}
}
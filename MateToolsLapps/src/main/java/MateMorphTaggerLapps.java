package org.lappsgrid.mate_tools_lapps;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.Morpheme;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.matetools.MatePosTagger;
import eu.openminted.share.annotations.api.constants.OperationType;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.fit.pipeline.SimplePipeline;

import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.matetools.MateLemmatizer;
import org.lappsgrid.api.ProcessingService;

import static org.apache.uima.fit.pipeline.SimplePipeline.*;
import org.apache.uima.fit.factory.*;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.*;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.resource.metadata.*;
import org.apache.uima.fit.factory.JCasFactory;

// import org.junit.Assume.*;

import de.tudarmstadt.ukp.dkpro.core.testing.*;

import org.apache.commons.io.FileUtils;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureStructureImplC;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.*;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.*;

import static org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;

// additional API for metadata
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;

import de.tudarmstadt.ukp.dkpro.core.testing.*;

import de.tudarmstadt.ukp.dkpro.core.matetools.MateMorphTagger;

import is2.mtag.Tagger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

public class MateMorphTaggerLapps implements ProcessingService {

    private String metadata;

    private JCas createJCas(String language, String document) throws org.apache.uima.UIMAException{
        AnalysisEngineDescription lemma = createEngineDescription(MateLemmatizer.class);
        AnalysisEngineDescription morphTag = createEngineDescription(MateMorphTagger.class);
        AnalysisEngineDescription posTag = createEngineDescription(MatePosTagger.class);

        AnalysisEngineDescription aggregate = createEngineDescription(lemma, posTag, morphTag);
        AnalysisEngine aEngine = createEngine(aggregate);
        return TestRunner.runTest(aEngine, language, document);
    }

    public MateMorphTaggerLapps() {
        metadata = generateMetadata();
        // AssumeResource.assumeResource(MateMorphTagger.class, "morphtagger", aLanguage, null);


        // TypeSystemDescription type_sys_desc = TypeSystemDescriptionFactory.createTypeSystemDescription();
    }

    private String generateMetadata() {
        ServiceMetadata metadata = new ServiceMetadata();

        metadata.setName(this.getClass().getName());
        metadata.setDescription("Cogroo Featurizer (LAPPS)");
        metadata.setVersion("1.0.0-SNAPSHOT");
        metadata.setVendor("http://www.lappsgrid.org");
        metadata.setLicense(Uri.APACHE2);

        IOSpecification requires = new IOSpecification();
        requires.addFormat(Uri.TEXT);
        requires.addFormat(Uri.LIF);
        requires.addLanguage("de");
        requires.addLanguage("es");
        requires.addLanguage("fr");
        requires.setEncoding("UTF-8");

        IOSpecification produces = new IOSpecification();
        produces.addFormat(Uri.LAPPS);
        produces.addAnnotation(Uri.TOKEN);
        produces.addLanguage("de");
        produces.addLanguage("es");
        produces.addLanguage("fr");
        produces.setEncoding("UTF-8");

        metadata.setRequires(requires);
        metadata.setProduces(produces);

        Data<ServiceMetadata> data = new Data<>(Uri.META, metadata);
        return data.asPrettyJson();
    }

    @Override
    public String getMetadata() {
        return metadata;
    }

    @Override
    public String execute(String input) {
        try {
            String[] lang_text = input.split("; ");
            Data input_text = new Data<>(Uri.TEXT, lang_text[1].trim());
            Data data = Serializer.parse(input_text.asJson(), Data.class);
            final String discriminator = data.getDiscriminator();
            if (discriminator.equals(Uri.ERROR)) {
                return input;
            }

            Container container = null;
            if (discriminator.equals(Uri.TEXT)) {
                container = new Container();
                container.setText(data.getPayload().toString());
            } else if (discriminator.equals(Uri.LAPPS)) {
                container = new Container((Map) data.getPayload());
            } else {
                String message = String.format("Unsupported discriminator type: %s", discriminator);
                return new Data<String>(Uri.ERROR, message).asJson();
            }

            View view = container.newView();
            String text = container.getText();
            // String[] words = text.trim().split("\\s+");
            // org.lappsgrid.mate_tools_lapps.MateMorphTaggerLAPPS.setJCas(aLanguage, aText);
            // System.out.println(jCas.getDocumentLanguage());
            try {

                // TokenBuilder<Token, Sentence> tb = new TokenBuilder<Token, Sentence>(Token.class,
                //    Sentence.class);
                // tb.buildTokens(jCas, aText);
                JCas newCas = createJCas(lang_text[0], lang_text[1].trim());
                // System.out.println(ae.getAnalysisEngineMetaData());
                int id = -1;
                for (Token tok : JCasUtil.select(newCas, Token.class)) {
                    int start = tok.getBegin();
                    int end = tok.getEnd();
                    Annotation a = view.newAnnotation("tok" + (++id), Uri.TOKEN, start, end);
                    String word = text.substring(start, end);
                    a.addFeature(Features.Token.WORD, word);
                    a.addFeature(Features.Token.LEMMA, tok.getLemmaValue());
                    a.addFeature(Features.Token.POS, tok.getPosValue());
                    MorphologicalFeatures morphFeatures = tok.getMorph();
                    String morphFeatValue = morphFeatures.getValue();
                    a.addFeature("morph_tag", morphFeatValue);
                    if (!morphFeatValue.equals("_")) {
                        String[] morphFeatSplit;
                        if (morphFeatValue.contains("|")) {
                            morphFeatSplit = morphFeatValue.split("\\|");
                        }else{
                            String[] oneLength = new String[1];
                            oneLength[0] = morphFeatValue;
                            morphFeatSplit = oneLength;
                        }
                        for (String morphFeat : morphFeatSplit) {
                            String[] individualFeat = morphFeat.split("=");
                            a.addFeature(individualFeat[0], individualFeat[1]);
                        }
                    }
                }
                view.addContains(Uri.TOKEN, this.getClass().getName(), "MateTools");

                data = new DataContainer(container);

                return data.asPrettyJson();

            } catch (UIMAException e2) {
                System.out.println("failed");
                e2.printStackTrace();
                return "failed";
            }
        } catch (PatternSyntaxException e) {
            String errString = "Invalid input pattern";
            System.err.println(errString);
            e.printStackTrace();
            return errString;
        }
    }
}

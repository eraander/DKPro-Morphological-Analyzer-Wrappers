package org.lappsgrid.rftagger_lapps;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.rftagger.RfTagger;
import de.tudarmstadt.ukp.dkpro.core.testing.TestRunner;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;

import java.util.Map;
import java.util.regex.PatternSyntaxException;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.lappsgrid.discriminator.Discriminators.Uri;

// import org.junit.Assume.*;
// additional API for metadata

public class RfTaggerLapps implements ProcessingService {

    private String metadata;

    private static JCas jCas;

    private static AnalysisEngine aEngine;

    private static String aLanguage;

    public JCas createJcas(String language, String variant, String document) throws ResourceInitializationException, UIMAException {
        AnalysisEngine rft = createEngine(RfTagger.class, RfTagger.PARAM_VARIANT, variant,
                RfTagger.PARAM_PRINT_TAGSET, true);
        JCas newJcas = TestRunner.runTest(rft, language, document);
        // RfTagger.getLogger();
        return newJcas;
    }

    public static AnalysisEngine getAnalysisEngine() {
        return aEngine;
    }

    public static void setJCas(String aLanguage, String aText) {
        jCas.setDocumentLanguage(aLanguage);
        jCas.setDocumentText(aText);
    }

    private void setNonNullFeature(Annotation a, String featureName, String featureValue){
        if(featureValue != null){
            a.addFeature(featureName, featureValue);
        }
    }

    public RfTaggerLapps() throws CASException, ResourceInitializationException, org.apache.uima.UIMAException {
        metadata = generateMetadata();
        // AssumeResource.assumeResource(MateMorphTagger.class, "morphtagger", aLanguage, null);

        // AnalysisEngineDescription rftagger = createEngineDescription(RfTagger.class, RfTagger.PARAM_VARIANT, null,
         //       RfTagger.PARAM_PRINT_TAGSET, true);
        // RfTagger.getLogger();
        // aEngine = createEngine(rftagger);
        // TypeSystemDescription type_sys_desc = TypeSystemDescriptionFactory.createTypeSystemDescription();
    }

    private String generateMetadata() {
        ServiceMetadata metadata = new ServiceMetadata();

        metadata.setName(this.getClass().getName());
        metadata.setDescription("RF Tagger (LAPPS)");
        metadata.setVersion("1.0.0-SNAPSHOT");
        metadata.setVendor("http://www.lappsgrid.org");
        metadata.setLicense(Uri.APACHE2);

        IOSpecification requires = new IOSpecification();
        requires.addFormat(Uri.TEXT);
        requires.addFormat(Uri.LIF);
        requires.addLanguage("cz");
        requires.addLanguage("de");
        requires.addLanguage("hu");
        requires.addLanguage("ru");
        requires.addLanguage("sk");
        requires.addLanguage("sl");
        requires.setEncoding("UTF-8");

        IOSpecification produces = new IOSpecification();
        produces.addFormat(Uri.LAPPS);
        produces.addAnnotation(Uri.TOKEN);
        produces.addLanguage("cz");
        produces.addLanguage("de");
        produces.addLanguage("hu");
        produces.addLanguage("ru");
        produces.addLanguage("sk");
        produces.addLanguage("sl");
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
            System.out.println(lang_text);
            Data input_text = new Data<>(Uri.TEXT, lang_text[2].trim());
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
                String variant = lang_text[1].trim();
                JCas newCas;
                if(variant.equals("null"))
                    newCas = createJcas(lang_text[0], null, lang_text[2].trim());
                else
                    newCas = createJcas(lang_text[0], variant, lang_text[2].trim());
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
                    a.addFeature("pos_mapped", tok.getPos().getType().getShortName());
                    MorphologicalFeatures morphFeatures = tok.getMorph();
                    setNonNullFeature(a, "animacy", morphFeatures.getAnimacy());
                    setNonNullFeature(a, "aspect", morphFeatures.getAspect());
                    setNonNullFeature(a, "case", morphFeatures.getCase());
                    setNonNullFeature(a, "definiteness", morphFeatures.getDefiniteness());
                    setNonNullFeature(a, "degree", morphFeatures.getDegree());
                    setNonNullFeature(a, "gender", morphFeatures.getGender());
                    setNonNullFeature(a, "mood", morphFeatures.getMood());
                    setNonNullFeature(a, "number", morphFeatures.getNumber());
                    setNonNullFeature(a, "num_type", morphFeatures.getNumType());
                    setNonNullFeature(a, "person", morphFeatures.getPerson());
                    setNonNullFeature(a, "pron_type", morphFeatures.getPronType());
                    setNonNullFeature(a, "possessive", morphFeatures.getPossessive());
                    setNonNullFeature(a, "reflex", morphFeatures.getReflex());
                    setNonNullFeature(a, "tense", morphFeatures.getTense());
                    setNonNullFeature(a, "voice", morphFeatures.getVoice());
                    setNonNullFeature(a, "verb_form", morphFeatures.getVerbForm());
                    setNonNullFeature(a, "morph_tag", morphFeatures.getValue());



                }
                view.addContains(Uri.TOKEN, this.getClass().getName(), "RfTagger");

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

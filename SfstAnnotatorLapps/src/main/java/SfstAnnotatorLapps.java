package org.lappsgrid.sfst_annotator_lapps;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.sfst.SfstAnnotator;
import de.tudarmstadt.ukp.dkpro.core.testing.TestRunner;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
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
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.lappsgrid.discriminator.Discriminators.Uri;

// import org.junit.Assume.*;
// additional API for metadata

public class SfstAnnotatorLapps implements ProcessingService {

    private String metadata;

    private static JCas jCas;


    private static String aLanguage;

    public static void setJCas(String aLanguage, String aText) {
        jCas.setDocumentLanguage(aLanguage);
        jCas.setDocumentText(aText);
    }

    private void setNonNullFeature(Annotation a, String featureName, String featureValue){
        if(featureValue != null){
            a.addFeature(featureName, featureValue);
        }
    }

    public JCas createJcas(String language, String variant, String document) throws ResourceInitializationException, UIMAException {
        AnalysisEngine sfst = createEngine(SfstAnnotator.class, SfstAnnotator.PARAM_VARIANT, variant,
                SfstAnnotator.PARAM_MODE, SfstAnnotator.Mode.ALL,
                SfstAnnotator.PARAM_PRINT_TAGSET, true);
        JCas newJcas = TestRunner.runTest(sfst, language, document);
        // RfTagger.getLogger();
        return newJcas;
    }

    public SfstAnnotatorLapps() throws CASException, ResourceInitializationException, org.apache.uima.UIMAException {
        metadata = generateMetadata();
        // AssumeResource.assumeResource(MateMorphTagger.class, "morphtagger", aLanguage, null);


        // TypeSystemDescription type_sys_desc = TypeSystemDescriptionFactory.createTypeSystemDescription();
    }

    private String generateMetadata() {
        ServiceMetadata metadata = new ServiceMetadata();

        metadata.setName(this.getClass().getName());
        metadata.setDescription("SFST Annotator (LAPPS)");
        metadata.setVersion("1.0.0-SNAPSHOT");
        metadata.setVendor("http://www.lappsgrid.org");
        metadata.setLicense(Uri.APACHE2);

        IOSpecification requires = new IOSpecification();
        requires.addFormat(Uri.TEXT);
        requires.addFormat(Uri.LIF);
        requires.addLanguage("de");
        requires.addLanguage("it");
        requires.addLanguage("tr");
        requires.setEncoding("UTF-8");

        IOSpecification produces = new IOSpecification();
        produces.addFormat(Uri.LAPPS);
        produces.addAnnotation(Uri.TOKEN);
        produces.addLanguage("de");
        produces.addLanguage("it");
        produces.addLanguage("tr");
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
                int id = 0;
                int sub_id = -1;
                int old_start = 0;
                for (MorphologicalFeatures morphFeatures : JCasUtil.select(newCas, MorphologicalFeatures.class)) {
                    int start = morphFeatures.getBegin();
                    int end = morphFeatures.getEnd();
                    if (start != old_start){
                        sub_id = -1;
                        ++id;
                    }
                    Annotation a = view.newAnnotation("tok" + (id) + "_" + (++sub_id), Uri.TOKEN, start, end);
                    String word = text.substring(start, end);
                    a.addFeature(Features.Token.WORD, word);
                    String morph_tag_value = morphFeatures.getValue();
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
                    setNonNullFeature(a, "morph_tag", morph_tag_value);

                    old_start = start;
                    if(morph_tag_value.contains("<")){
                        String[] tags = morph_tag_value.split("<");
                        String lemma = "";
                        String probable_tag = "";
                        String last_tagchain = "";
                        boolean has_end_tag_last = true;
                        for (int i = 0; i < tags.length; i++) {
                            while (tags[i].equals("")) i++;
                            String tag = tags[i];
                            if ((tag.indexOf(">") != tag.length() - 1) || (tag.contains("^"))) {
                                String tag_modified = "<" + tag;
                                if (tag.contains(">"))
                                    lemma += last_tagchain + tag_modified;
                                else lemma += last_tagchain + tag;
                                last_tagchain = "";
                                has_end_tag_last = false;
                                // tag = tags[i];
                            }else{
                                if(!has_end_tag_last){
                                    lemma += last_tagchain;
                                    probable_tag = tag;
                                    last_tagchain = "<" + tag;
                                }else{
                                    last_tagchain += "<" + tag;
                                    // else last_tagchain += tag;
                                }
                                has_end_tag_last = true;
                            }
                        }
                        a.addFeature(Features.Token.LEMMA, lemma);
                        a.addFeature(Features.Token.POS, "<" + probable_tag);
                    }else{
                        a.addFeature(Features.Token.LEMMA, "");
                        a.addFeature(Features.Token.POS, "");
                    }


                }
                view.addContains(Uri.TOKEN, this.getClass().getName(), "SfstAnnotatorLapps");

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

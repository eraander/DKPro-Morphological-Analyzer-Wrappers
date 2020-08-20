import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.junit.Assert.*;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.matetools.MateLemmatizer;
import org.dkpro.core.matetools.MateMorphTagger;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.testing.*;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import org.lappsgrid.api.WebService;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MateMorphTaggerLappsTest
{
    @Rule
    public DkproTestContext testContext = new DkproTestContext();

    // this will be the sandbag
    protected WebService service;

    // initiate the service before each test
    @Before
    public void setUp() throws IOException, ResourceInitializationException, CASException, UIMAException {
        service = new org.lappsgrid.mate_tools_lapps.MateMorphTaggerLapps();
    }

    // then destroy it after the test
    @After
    public void tearDown() {
        service = null;
    }


    @Test
    public void testMetadata() {
        String json = service.getMetadata();
        assertNotNull("service.getMetadata() returned null", json);

        Data data = Serializer.parse(json, Data.class);
        assertNotNull("Unable to parse metadata json.", data);
        assertNotSame(data.getPayload().toString(), Discriminators.Uri.ERROR, data.getDiscriminator());

        ServiceMetadata metadata = new ServiceMetadata((Map) data.getPayload());

        assertEquals("Vendor is not correct", "http://www.lappsgrid.org", metadata.getVendor());
        assertEquals("Name is not correct", org.lappsgrid.mate_tools_lapps.MateMorphTaggerLapps.class.getName(), metadata.getName());
        assertEquals("Version is not correct.","1.0.0-SNAPSHOT" , metadata.getVersion());
        assertEquals("License is not correct", Discriminators.Uri.APACHE2, metadata.getLicense());

        IOSpecification produces = metadata.getProduces();
        assertEquals("Produces encoding is not correct", "UTF-8", produces.getEncoding());
        assertEquals("Too many annotation types produced", 1, produces.getAnnotations().size());
        assertEquals("Tokens not produced", Discriminators.Uri.TOKEN, produces.getAnnotations().get(0));
        assertEquals("Too many output formats", 1, produces.getFormat().size());
        assertEquals("LIF not produces", Discriminators.Uri.LAPPS, produces.getFormat().get(0));

        IOSpecification requires = metadata.getRequires();
        assertEquals("Requires encoding is not correct", "UTF-8", requires.getEncoding());
        List<String> list = requires.getFormat();
        assertTrue("LIF format not accepted.", list.contains(Discriminators.Uri.LAPPS));
        assertTrue("Text not accepted", list.contains(Discriminators.Uri.TEXT));
        list = requires.getAnnotations();
        assertEquals("Required annotations should be empty", 0, list.size());
    }

    @Test
    public void testGerman()
            throws Exception
    {

        // Assume.assumeTrue(Runtime.getRuntime().maxMemory() >= 1000000000);
        String text = "Wir brauchen ein sehr kompliziertes Beispiel , welches möglichst viele Konstituenten und Dependenzen beinhaltet .";
        String input = "de;  " + text;
        String data = this.service.execute(input);

        long[] starts = {0L, 4L, 13L, 17L, 22L, 36L, 45L, 47L, 55L, 65L, 71L, 85L, 89L, 101L, 112L};
        long[] ends = {3L, 12L, 16L, 21L, 35L, 44L, 46L, 54L, 64L, 70L, 84L, 88L, 100L, 111L, 113L};

        String[] posOriginal = { "PPER", "VVFIN", "ART", "ADV", "ADJA", "NN", "$,", "PRELS", "ADV",
                "PIAT", "NN", "KON", "NN", "VVFIN", "$." };

        String[] posMapped = { "POS_PRON", "POS_VERB", "POS_DET", "POS_ADV", "POS_ADJ", "POS_NOUN", "POS_PUNCT", "POS_PRON", "POS_ADV",
                "POS_PRON", "POS_NOUN", "POS_CONJ", "POS_NOUN", "POS_VERB", "POS_PUNCT" };

        String[] words = {"Wir", "brauchen", "ein", "sehr", "kompliziertes", "Beispiel", ",", "welches", "möglichst",
        "viele", "Konstituenten", "und", "Dependenzen", "beinhaltet", "."};

        String[] lemmas = {"wir", "brauchen", "ein", "sehr", "kompliziert", "beispiel", "--", "welcher", "möglichst",
        "vieler", "konstituent", "und", "dependenz", "beinhalten", "--"};

        String[] morph_tags = {"case=nom|number=pl|gender=*|person=1", "number=pl|person=1|tense=pres|mood=ind",
        "case=acc|number=sg|gender=neut", "_", "case=acc|number=sg|gender=neut|degree=pos", "case=acc|number=sg|gender=neut",
        "_", "case=acc|number=sg|gender=neut", "_", "case=acc|number=pl|gender=*", "case=acc|number=pl|gender=*", "_",
        "case=acc|number=pl|gender=fem", "number=sg|person=3|tense=pres|mood=ind", "_"};

        String[] cases = {"nom", null, "acc", null, "acc", "acc", null, "acc", null, "acc", "acc", null, "acc", null, null};
        String[] numbers = {"pl", "pl", "sg", null, "sg", "sg", null, "sg", null, "pl", "pl", null, "pl", "sg", null};
        String[] genders = {"*", null, "neut", null, "neut", "neut", null, "neut", null, "*", "*", null, "fem", null, null};
        String[] persons = {"1", "1", null, null, null, null, null, null, null, null, null, null, null, "3", null};
        String[] tenses = {null, "pres", null, null, null, null, null, null, null, null, null, null, null, "pres", null};
        String[] moods = {null, "ind", null, null, null, null, null, null, null, null, null, null, null, "ind", null};
        String[] degrees = {null, null, null, null, "pos", null, null, null, null, null, null, null, null, null, null};

        // System.out.println(data);
        // Assert.assertEquals("e", "e");
        Container container = Serializer.parse(data, DataContainer.class).getPayload();
        assertEquals("Text not set correctly", text, container.getText());

        // Now, see all annotations in current view is correct
        List<View> views = container.getViews();
        if (views.size() != 1) {
            fail(String.format("Expected 1 view. Found: %d", views.size()));
        }
        View view = views.get(0);
        assertTrue("View does not contain tokens", view.contains(Discriminators.Uri.TOKEN));
        List<Annotation> annotations = view.getAnnotations();
        if (annotations.size() != 15) {
            fail(String.format("Expected 15 annotations. Found %d", annotations.size()));
        }

        for (int i = 0; i < annotations.size(); i++){
            Annotation token = annotations.get(i);
            assertEquals("Token " + i + ": wrong start", starts[i], token.getStart().longValue());
            assertEquals("Token " + i + ": wrong end", ends[i], token.getEnd().longValue());
            assertEquals("Token " + i + ": wrong word", words[i], token.getFeature(Features.Token.WORD));
            assertEquals("Token " + i + ": wrong lemma", lemmas[i], token.getFeature(Features.Token.LEMMA));
            assertEquals("Token " + i + ": wrong morph tag", morph_tags[i], token.getFeature("morph_tag"));
            assertEquals("Token " + i + ": wrong case", cases[i], token.getFeature("case"));
            assertEquals("Token " + i + ": wrong degree", degrees[i], token.getFeature("degree"));
            assertEquals("Token " + i + ": wrong number",  numbers[i], token.getFeature("number"));
            assertEquals("Token " + i + ": wrong gender", genders[i], token.getFeature("gender"));
            assertEquals("Token " + i + ": wrong mood", moods[i], token.getFeature("mood"));
            assertEquals("Token " + i + ": wrong person", persons[i], token.getFeature("person"));
            assertEquals("Token " + i + ": wrong tense", tenses[i], token.getFeature("tense"));
            assertEquals("Token " + i + ": wrong pos", posOriginal[i], token.getFeature(Features.Token.POS));
        }
    }

    @Test
    public void testFrench()
            throws Exception
    {
        // Assume.assumeTrue(Runtime.getRuntime().maxMemory() >= 1000000000);
        String text =  "Nous avons besoin d'une phrase par exemple très "
            + "compliqué, qui contient des constituants que de nombreuses dépendances et que "
            + "possible .";
        String input = "fr; " + text;
        String data = this.service.execute(input);

        long[] starts = {0L, 5L, 11L, 18L, 24L, 31L, 35L, 43L, 48L, 59L, 63L, 72L, 76L, 89L, 93L, 96L, 107L, 119L, 122L, 126L, 135L};
        long[] ends = {4L, 10L, 17L, 23L, 30L, 34L, 42L, 47L, 58L, 62L, 71L, 75L, 88L, 92L, 95L, 106L, 118L, 121L, 125L, 134L, 136L};

        String[] posMapped = { "POS_PRON", "POS_VERB", "POS_NOUN", "POS_ADP", "POS_NOUN", "POS_ADP",
                "POS_NOUN", "POS_ADV", "POS_ADJ", "POS_PRON", "POS_VERB", "POS_DET", "POS_NOUN",
                "POS_CONJ", "POS_DET", "POS_ADJ", "POS_NOUN", "POS_CONJ", "POS_CONJ", "POS_ADJ",
                "POS_PUNCT" };

        String[] posOriginal = { "CLS", "V", "NC", "P", "NC", "P", "NC", "ADV", "ADJ", "PROREL",
                "V", "DET", "NC", "CS", "DET", "ADJ", "NC", "CC", "CS", "ADJ", "PONCT" };

        String[] words = {"Nous", "avons", "besoin", "d\'une", "phrase", "par", "exemple", "très", "compliqué,",
                "qui", "contient", "des", "constituants", "que", "de", "nombreuses", "dépendances", "et", "que", "possible", "."};

        String[] lemmas = {"il", "avoir", "besoin", "d\'une", "phrase", "par", "exemple", "très", "compliqué,",
                "qui", "contenir", "de", "constituant", "que", "de", "nombreux", "dépendance", "et", "que", "possible", "."};

        String[] morph_tags = {"g=m|n=p|p=1|s=suj", "m=ind|n=p|p=1|t=pst", "g=m|n=s|s=c", "_", "g=f|n=s|s=c",
                "_", "g=m|n=s|s=c", "_", "g=m|n=s|s=qual", "g=m|n=p|p=3|s=rel", "m=ind|n=s|p=3|t=pst", "g=m|n=p|s=ind",
                "g=m|n=p|s=c", "g=m|n=p|p=3|s=rel", "g=f|n=p|s=ind", "g=f|n=p|s=qual", "g=f|n=p|s=c", "s=c", "s=s",
                "g=m|n=s|s=qual", "s=s"};


        // String[] cases = {"nom", null, "acc", null, "acc", "acc", null, "acc", null, "acc", "acc", null, "acc", null, null};
        String[] numbers = {"p", "p", "s", null, "s", null, "s", null, "s", "p", "s", "p", "p", "p", "p", "p", "p", null, null, "s", null};
        String[] genders = {"m", null, "m", null, "f", null, "m", null, "m", "m", null, "m", "m", "m", "f", "f", "f", null, null, "m", null};
        String[] persons = {"1", "1", null, null, null, null, null, null, null, "3", "3", null, null, "3", null, null, null, null, null, null, null};
        String[] tenses = {null, "pst", null, null, null, null, null, null, null, null, "pst", null, null, null, null, null, null, null, null, null, null};
        String[] moods = {null, "ind", null, null, null, null, null, null, null, null, "ind", null, null, null, null, null, null, null, null, null, null};
        String[] s = {"suj", null, "c", null, "c", null, "c", null, "qual", "rel", null, "ind", "c", "rel", "ind", "qual", "c", "c", "s", "qual", "s"};

        // System.out.println(data);
        // Assert.assertEquals("e", "e");
        Container container = Serializer.parse(data, DataContainer.class).getPayload();
        assertEquals("Text not set correctly", text.trim(), container.getText());

        // Now, see all annotations in current view is correct
        List<View> views = container.getViews();
        if (views.size() != 1) {
            fail(String.format("Expected 1 view. Found: %d", views.size()));
        }
        View view = views.get(0);
        assertTrue("View does not contain tokens", view.contains(Discriminators.Uri.TOKEN));
        List<Annotation> annotations = view.getAnnotations();
        if (annotations.size() != 21) {
            fail(String.format("Expected 21 annotations. Found %d", annotations.size()));
        }

        for (int i = 0; i < annotations.size(); i++){
            Annotation token = annotations.get(i);
            assertEquals("Token " + i + ": wrong start", starts[i], token.getStart().longValue());
            assertEquals("Token " + i + ": wrong end", ends[i], token.getEnd().longValue());
            assertEquals("Token " + i + ": wrong word", words[i], token.getFeature(Features.Token.WORD));
            assertEquals("Token " + i + ": wrong lemma", lemmas[i], token.getFeature(Features.Token.LEMMA));
            assertEquals("Token " + i + ": wrong morph tag", morph_tags[i], token.getFeature("morph_tag"));
            assertEquals("Token " + i + ": wrong gender", genders[i], token.getFeature("g"));
            assertEquals("Token " + i + ": wrong mood", moods[i], token.getFeature("m"));
            assertEquals("Token " + i + ": wrong number",  numbers[i], token.getFeature("n"));
            assertEquals("Token " + i + ": wrong person", persons[i], token.getFeature("p"));
            assertEquals("Token " + i + ": wrong s", s[i], token.getFeature("s"));
            assertEquals("Token " + i + ": wrong tense", tenses[i], token.getFeature("t"));
            assertEquals("Token " + i + ": wrong pos", posOriginal[i], token.getFeature(Features.Token.POS));
        }


        // System.out.println(data);
    }

    @Test
    public void testSpanish()
            throws EOFException
    {
        // .assumeTrue(Runtime.getRuntime().maxMemory() >= 1000000000);
        String text = "Necesitamos una oración de ejemplo muy complicado , que "
            + "contiene la mayor cantidad de componentes y dependencias como sea posible .";
        String input = "es;  " + text;

        String data = this.service.execute(input);
        // System.out.println(data);

        long[] starts = {0L, 12L, 16L, 24L, 27L, 35L, 39L, 50L, 52L, 56L, 65L, 68L, 74L, 83L, 86L, 98L, 100L, 113L, 118L, 122L, 130L};
        long[] ends = {11L, 15L, 23L, 26L, 34L, 38L, 49L, 51L, 55L, 64L, 67L, 73L, 82L, 85L, 97L, 99L, 112L, 117L, 121L, 129L, 131L};


        String[] posOriginal = { "v", "d", "n", "s", "n", "r", "a", "f", "p", "v", "d", "a", "n", "s", "n", "c", "n", "c", "v", "a", "f"};

        String[] words = {"Necesitamos", "una", "oración", "de", "ejemplo", "muy", "complicado", ",", "que",
                "contiene", "la", "mayor", "cantidad", "de", "componentes", "y", "dependencias", "como", "sea", "posible", "."};

        String[] lemmas = {"necesitar", "uno", "oración", "de", "ejemplo", "mucho", "complicado", ",", "que",
                "contener", "el", "mayor", "cantidad", "de", "componente", "y", "dependencia", "como", "ser", "posible", "."};

        String[] morph_tags = {"postype=main|gen=c|num=p|person=1|mood=indicative|tense=present", "postype=indefinite|gen=f|num=s",
                "postype=common|gen=f|num=s", "postype=preposition|gen=c|num=c", "postype=common|gen=m|num=s",
                "_", "postype=qualificative|gen=m|num=s|posfunction=participle", "punct=comma", "postype=relative|gen=c|num=c",
                "postype=main|gen=c|num=s|person=3|mood=indicative|tense=present", "postype=article|gen=f|num=s",
                "postype=qualificative|gen=c|num=s", "postype=common|gen=f|num=s", "postype=preposition|gen=c|num=c",
                "postype=common|gen=m|num=p", "postype=coordinating", "postype=common|gen=f|num=p", "postype=subordinating",
                "postype=semiauxiliary|gen=c|num=s|person=3|mood=subjunctive|tense=present", "postype=qualificative|gen=c|num=s", "punct=period"};

        String[] numbers = {"p", "s", "s", "c", "s", null, "s", null, "c", "s", "s", "s", "s", "c", "p", null, "p", null, "s", "s", null};
        String[] genders = {"c", "f", "f", "c", "m", null, "m", null, "c", "c", "f", "c", "f", "c", "m", null, "f", null, "c", "c", null};
        String[] persons = {"1", null, null, null, null, null, null, null, null, "3", null, null, null, null, null, null, null, null, "3", null, null};
        String[] tenses = {"present", null, null, null, null, null, null, null, null, "present", null, null, null,
                null, null, null, null, null, "present", null, null};
        String[] moods = {"indicative", null, null, null, null, null, null, null, null, "indicative",
                null, null, null, null, null, null, null, null, "subjunctive", null, null};
        String[] postypes = {"main", "indefinite", "common", "preposition", "common", null, "qualificative", null,
                "relative", "main", "article", "qualificative", "common", "preposition", "common", "coordinating",
                "common", "subordinating", "semiauxiliary", "qualificative", null};
        String[] posfunctions = {null, null, null, null, null, null, "participle", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null};
        String[] puncts = {null, null, null, null, null, null, null, "comma", null, null, null, null, null,
                null, null, null, null, null, null, null, "period"};



        Container container = Serializer.parse(data, DataContainer.class).getPayload();
        assertEquals("Text not set correctly", text.trim(), container.getText());

        // Now, see all annotations in current view is correct
        List<View> views = container.getViews();
        if (views.size() != 1) {
            fail(String.format("Expected 1 view. Found: %d", views.size()));
        }
        View view = views.get(0);
        assertTrue("View does not contain tokens", view.contains(Discriminators.Uri.TOKEN));
        List<Annotation> annotations = view.getAnnotations();
        if (annotations.size() != 21) {
            fail(String.format("Expected 21 annotations. Found %d", annotations.size()));
        }


        for (int i = 0; i < annotations.size(); i++){
            Annotation token = annotations.get(i);
            assertEquals("Token " + i + ": wrong start", starts[i], token.getStart().longValue());
            assertEquals("Token " + i + ": wrong end", ends[i], token.getEnd().longValue());
            assertEquals("Token " + i + ": wrong word", words[i], token.getFeature(Features.Token.WORD));
            assertEquals("Token " + i + ": wrong lemma", lemmas[i], token.getFeature(Features.Token.LEMMA));
            assertEquals("Token " + i + ": wrong morph tag", morph_tags[i], token.getFeature("morph_tag"));
            assertEquals("Token " + i + ": wrong gender", genders[i], token.getFeature("gen"));
            assertEquals("Token " + i + ": wrong mood", moods[i], token.getFeature("mood"));
            assertEquals("Token " + i + ": wrong number",  numbers[i], token.getFeature("num"));
            assertEquals("Token " + i + ": wrong person", persons[i], token.getFeature("person"));
            assertEquals("Token " + i + ": wrong postype", postypes[i], token.getFeature("postype"));
            assertEquals("Token " + i + ": wrong posfunction", posfunctions[i], token.getFeature("posfunction"));
            assertEquals("Token " + i + ": wrong punct", puncts[i], token.getFeature("punct"));
            assertEquals("Token " + i + ": wrong tense", tenses[i], token.getFeature("tense"));
            assertEquals("Token " + i + ": wrong pos", posOriginal[i], token.getFeature(Features.Token.POS));
        }
    }
}

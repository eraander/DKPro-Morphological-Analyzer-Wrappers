/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASException;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


public class SfstAnnotatorLappsTest {
    @Rule
    public DkproTestContext testContext = new DkproTestContext();

    protected WebService service;

    // initiate the service before each test
    @Before
    public void setUp() throws IOException, ResourceInitializationException, CASException, UIMAException {
        service = new org.lappsgrid.sfst_annotator_lapps.SfstAnnotatorLapps();
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
        assertEquals("Name is not correct", org.lappsgrid.sfst_annotator_lapps.SfstAnnotatorLapps.class.getName(), metadata.getName());
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
    public void testTurkish()
        throws Exception
    {
        String input_string = "tr; trmorph-ca; Doktor hastane çalış .";
        String data = service.execute(input_string);
        // System.out.println(data);
        Container container = Serializer.parse(data, DataContainer.class).getPayload();
        assertEquals("Text not set correctly", "Doktor hastane çalış .", container.getText());

        // Now, see all annotations in current view is correct
        List<View> views = container.getViews();
        if (views.size() != 1) {
            fail(String.format("Expected 1 view. Found: %d", views.size()));
        }
        View view = views.get(0);
        assertTrue("View does not contain tokens", view.contains(Discriminators.Uri.TOKEN));
        List<Annotation> annotations = view.getAnnotations();
        if (annotations.size() != 13) {
            fail(String.format("Expected 13 annotations. Found %d", annotations.size()));
        }

        long[] starts = {0L, 7L, 7L, 7L, 15L, 15L, 15L, 15L, 15L, 15L, 15L, 15L, 21L};
        long[] ends = {6L, 14L, 14L, 14L, 20L, 20L, 20L, 20L, 20L, 20L, 20L, 20L, 22L};
        String[] words = {"Doktor", "hastane", "hastane", "hastane", "çalış", "çalış", "çalış", "çalış", "çalış", "çalış", "çalış", "çalış", "."};
        String[] cases = {null, null, null, null, null, null, null, null, null, null, null, null, null};
        String[] lemmas = {"", "hastane", "hastane", "hastane", "çal", "çal", "çal", "çal", "çal", "çal", "çalış", "çalış", "."};
        String[] pos_tags = {"", "<n>", "<n>", "<n>", "<v>", "<v>", "<v>", "<v>", "<v>", "<v>", "<v>", "<v>", "<pnct>"};
        String[] morph_tags = {"", "hastane<n>", "hastane<n><3s>", "hastane<n><3p>", "çal<v><vn_yis>", "çal<v><vn_yis><3p>",
                "çal<v><vn_yis><3s>", "çal<v><D_yIS><n>", "çal<v><D_yIS><n><3s>", "çal<v><D_yIS><n><3p>",
        "çalış<v><t_imp><3p>", "çalış<v><t_imp><2s>", ".<pnct>"};
        String[] ids = {"0_0", "1_0", "1_1", "1_2", "2_0", "2_1", "2_2", "2_3", "2_4", "2_5", "2_6", "2_7", "3_0"};
        String[] numbers = {null, null, "Sing", "Plur", null, "Plur", "Sing", null, "Sing", "Plur", "Plur", "Sing", null};
        String[] genders = {null, null, null, null, null, null, null, null, null, null, null, null, null};
        String[] moods = {null, null, null, null, null, null, null, null, null, null, null, null, null};
        String[] persons = {null, null, "3", "3", null, "3", "3", null, "3", "3", "3", "2", null};
        String[] tenses = {null, null, null, null, null, null, null, null, null, null, null, null, null};

        for (int i = 0; i < annotations.size(); i++){
            Annotation token = annotations.get(i);
            assertEquals("Token " + ids[i] + ": wrong start", starts[i], token.getStart().longValue());
            assertEquals("Token " + ids[i] + ": wrong end", ends[i], token.getEnd().longValue());
            assertEquals("Token " + ids[i] + ": wrong word", words[i], token.getFeature(Features.Token.WORD));
            assertEquals("Token " + ids[i] + ": wrong case", cases[i], token.getFeature("case"));
            assertEquals("Token " + ids[i] + ": wrong lemma", lemmas[i], token.getFeature(Features.Token.LEMMA));
            assertEquals("Token " + ids[i] + ": wrong pos tag" + token, pos_tags[i], token.getFeature(Features.Token.POS));
            assertEquals("Token " + ids[i] + ": wrong morph tag", morph_tags[i], token.getFeature("morph_tag"));
            assertEquals("Token " + ids[i] + ": wrong number",  numbers[i], token.getFeature("number"));
            assertEquals("Token " + ids[i] + ": wrong gender", genders[i], token.getFeature("gender"));
            assertEquals("Token " + ids[i] + ": wrong mood", moods[i], token.getFeature("mood"));
            assertEquals("Token " + ids[i] + ": wrong person", persons[i], token.getFeature("person"));
            assertEquals("Token " + ids[i] + ": wrong tense", tenses[i], token.getFeature("tense"));
            assertEquals("Token " + ids[i] + ": wrong id", "tok" + ids[i], token.getId());
        }
    }


    @Test
    public void testGermanMorphisto()
        throws Exception
    {
        String input_string = "de; morphisto-ca; Der Arzt arbeitet im Krankenhaus .";
        String data = service.execute(input_string);
        long[] starts = {0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 4L, 4L, 4L, 9L, 9L, 9L, 9L, 18L, 18L, 21L, 21L, 21L, 21L,
        21L, 21L, 21L, 21L, 21L, 21L, 21L, 21L, 33L};
        long[] ends = {3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 8L, 8L, 8L, 17L, 17L, 17L, 17L, 20L, 20L, 32L, 32L, 32L, 32L,
        32L, 32L, 32L, 32L, 32L, 32L, 32L, 32L, 34L};
        String[] cases = {"Gen", "Dat", "Gen", "Dat", "Gen", "Dat", "Nom", "Nom", "Nom", "Nom", "Dat", "Acc", null,
        null, null, null, "Dat", "Dat", "Nom", "Dat", "Acc", "Nom", "Dat", "Acc", "Nom", "Dat", "Acc", "Nom", "Dat",
        "Acc", null};
        String[] words = {"Der", "Der", "Der", "Der", "Der", "Der", "Der", "Der", "Der", "Arzt", "Arzt", "Arzt",
        "arbeitet", "arbeitet", "arbeitet", "arbeitet", "im", "im", "Krankenhaus", "Krankenhaus", "Krankenhaus",
                "Krankenhaus", "Krankenhaus", "Krankenhaus", "Krankenhaus", "Krankenhaus", "Krankenhaus", "Krankenhaus",
                "Krankenhaus", "Krankenhaus", "."};
        String[] ids = {"0_0", "0_1", "0_2", "0_3", "0_4", "0_5", "0_6", "0_7", "0_8", "1_0", "1_1", "1_2", "2_0",
        "2_1", "2_2", "2_3", "3_0", "3_1", "4_0", "4_1", "4_2", "4_3", "4_4", "4_5", "4_6", "4_7", "4_8", "4_9", "4_10",
        "4_11", "5_0"};
        String[] genders = {"Fem", "Fem", null, "Fem", "Fem", "Fem", "Masc", "Masc", "Masc", "Masc", "Masc", "Masc",
        null, null, null, null, "Masc", "Neut", "Neut", "Neut", "Neut", "Neut", "Neut", "Neut", "Neut", "Neut", "Neut",
        "Neut", "Neut", "Neut", null};
        String[] moods = {null, null, null, null, null, null, null, null, null, null, null, null, "Sub", null, "Ind",
        "Ind", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};
        String[] numbers = {"Sing", "Sing", "Plur", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing",
                "Sing", "Plur", "Plur", "Plur", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing",
                "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", null};
        String[] persons = {null, null, null, null, null, null, null, null, null, null, null, null, "2", null, "2",
                "3", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};
        String[] tenses = {null, null, null, null, null, null, null, null, null, null, null, null, "Pres", "Imp", "Pres",
                "Pres", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};

        String[] morph_tags = {"<CAP>die<+ART><Def><Fem><Gen><Sg>", "<CAP>die<+ART><Def><Fem><Dat><Sg>",
        "<CAP>die<+ART><Def><NoGend><Gen><Pl>", "<CAP>die<+REL><subst><Fem><Dat><Sg>", "<CAP>die<+DEM><subst><Fem><Gen><Sg>",
        "<CAP>die<+DEM><subst><Fem><Dat><Sg>", "<CAP>der<+ART><Def><Masc><Nom><Sg>", "<CAP>der<+REL><subst><Masc><Nom><Sg>",
        "<CAP>der<+DEM><subst><Masc><Nom><Sg>", "Arzt<+NN><Masc><Nom><Sg>", "Arzt<+NN><Masc><Dat><Sg>", "Arzt<+NN><Masc><Akk><Sg>",
        "arbeiten<+V><2><Pl><Pres><Konj>", "arbeiten<+V><Imp><Pl>", "arbeiten<+V><2><Pl><Pres><Ind>", "arbeiten<+V><3><Sg><Pres><Ind>",
        "im<+PREP/ART><Masc><Dat><Sg>", "im<+PREP/ART><Neut><Dat><Sg>", "Kranke<NN>Haus<+NN><Neut><Nom><Sg>",
        "Kranke<NN>Haus<+NN><Neut><Dat><Sg>", "Kranke<NN>Haus<+NN><Neut><Akk><Sg>", "Krankenhaus<+NN><Neut><Nom><Sg>",
        "Krankenhaus<+NN><Neut><Dat><Sg>", "Krankenhaus<+NN><Neut><Akk><Sg>", "kranken<V><NN><SUFF>Haus<+NN><Neut><Nom><Sg>",
        "kranken<V><NN><SUFF>Haus<+NN><Neut><Dat><Sg>", "kranken<V><NN><SUFF>Haus<+NN><Neut><Akk><Sg>",
        "krank<ADJ><NN><SUFF>Haus<+NN><Neut><Nom><Sg>", "krank<ADJ><NN><SUFF>Haus<+NN><Neut><Dat><Sg>",
        "krank<ADJ><NN><SUFF>Haus<+NN><Neut><Akk><Sg>", ".<+IP><Norm>"};

        String[] pos_tags = {"<+ART>", "<+ART>", "<+ART>", "<+REL>", "<+DEM>", "<+DEM>", "<+ART>", "<+REL>", "<+DEM>",
        "<+NN>", "<+NN>", "<+NN>", "<+V>", "<+V>", "<+V>", "<+V>", "<+PREP/ART>", "<+PREP/ART>", "<+NN>",
        "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+IP>"};

        String[] lemmas = {"<CAP>die", "<CAP>die", "<CAP>die", "<CAP>die", "<CAP>die", "<CAP>die", "<CAP>der", "<CAP>der",
        "<CAP>der", "Arzt", "Arzt", "Arzt", "arbeiten", "arbeiten", "arbeiten", "arbeiten", "im", "im", "Kranke<NN>Haus",
        "Kranke<NN>Haus", "Kranke<NN>Haus", "Krankenhaus", "Krankenhaus", "Krankenhaus", "kranken<V><NN><SUFF>Haus",
        "kranken<V><NN><SUFF>Haus", "kranken<V><NN><SUFF>Haus", "krank<ADJ><NN><SUFF>Haus", "krank<ADJ><NN><SUFF>Haus",
        "krank<ADJ><NN><SUFF>Haus", "."};

        Container container = Serializer.parse(data, DataContainer.class).getPayload();
        assertEquals("Text not set correctly", "Der Arzt arbeitet im Krankenhaus .", container.getText());

        // Now, see all annotations in current view is correct
        List<View> views = container.getViews();
        if (views.size() != 1) {
            fail(String.format("Expected 1 view. Found: %d", views.size()));
        }
        View view = views.get(0);
        assertTrue("View does not contain tokens", view.contains(Discriminators.Uri.TOKEN));
        List<Annotation> annotations = view.getAnnotations();
        if (annotations.size() != 31) {
            fail(String.format("Expected 31 annotations. Found %d", annotations.size()));
        }

        for (int i = 0; i < annotations.size(); i++){
            Annotation token = annotations.get(i);
            assertEquals("Token " + ids[i] + ": wrong start", starts[i], token.getStart().longValue());
            assertEquals("Token " + ids[i] + ": wrong end", ends[i], token.getEnd().longValue());
            assertEquals("Token " + ids[i] + ": wrong word", words[i], token.getFeature(Features.Token.WORD));
            assertEquals("Token " + ids[i] + ": wrong case", cases[i], token.getFeature("case"));
            assertEquals("Token " + ids[i] + ": wrong lemma", lemmas[i], token.getFeature(Features.Token.LEMMA));
            assertEquals("Token " + ids[i] + ": wrong pos tag" + token, pos_tags[i], token.getFeature(Features.Token.POS));
            assertEquals("Token " + ids[i] + ": wrong morph tag", morph_tags[i], token.getFeature("morph_tag"));
            assertEquals("Token " + ids[i] + ": wrong number",  numbers[i], token.getFeature("number"));
            assertEquals("Token " + ids[i] + ": wrong gender", genders[i], token.getFeature("gender"));
            assertEquals("Token " + ids[i] + ": wrong mood", moods[i], token.getFeature("mood"));
            assertEquals("Token " + ids[i] + ": wrong person", persons[i], token.getFeature("person"));
            assertEquals("Token " + ids[i] + ": wrong tense", tenses[i], token.getFeature("tense"));
            assertEquals("Token " + ids[i] + ": wrong id", "tok" + ids[i], token.getId());
        }



    }
    @Test
    public void testGermanSmor()
        throws Exception
    {
        String input_string = "de; smor-ca; Der Arzt arbeitet im Krankenhaus .";
        String data = service.execute(input_string);
        long[] starts = {0L, 4L, 4L, 4L, 9L, 9L, 9L, 9L, 18L, 18L, 21L, 21L, 21L, 21L, 21L, 21L, 21L, 21L, 21L, 33L};
        long[] ends = {3L, 8L, 8L, 8L, 17L, 17L, 17L, 17L, 20L, 20L, 32L, 32L, 32L, 32L, 32L, 32L, 32L, 32L, 32L, 34L};
        String[] cases = {null, "Nom", "Dat", "Acc", null, null, null, null, "Dat", "Dat", "Nom", "Dat", "Acc", "Nom",
                "Dat", "Acc", "Nom", "Dat", "Acc", null};
        String[] words = {"Der", "Arzt", "Arzt", "Arzt", "arbeitet", "arbeitet", "arbeitet", "arbeitet", "im", "im",
                "Krankenhaus", "Krankenhaus", "Krankenhaus", "Krankenhaus", "Krankenhaus", "Krankenhaus", "Krankenhaus",
                "Krankenhaus", "Krankenhaus", "."};
        String[] ids = {"0_0", "1_0", "1_1", "1_2", "2_0", "2_1", "2_2", "2_3", "3_0", "3_1", "4_0", "4_1", "4_2",
                "4_3", "4_4", "4_5", "4_6", "4_7", "4_8", "5_0"};
        String[] genders = {null, "Masc", "Masc", "Masc", null, null, null, null, "Masc", "Neut", "Neut", "Neut", "Neut", "Neut", "Neut", "Neut",
                "Neut", "Neut", "Neut", null};
        String[] moods = {null, null, null, null, "Sub", "Ind", "Imp",
                "Ind", null, null, null, null, null, null, null, null, null, null, null, null};
        String[] numbers = {null, "Sing", "Sing", "Sing", "Plur", "Plur", "Plur", "Sing", "Sing", "Sing", "Sing", "Sing",
                "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", null};
        String[] persons = {null, null, null, null, "2", "2", null,
                "3", null, null, null, null, null, null, null, null, null, null, null, null};
        String[] tenses = {null, null, null, null, "Pres", "Pres", null,
                "Pres", null, null, null, null, null, null, null, null, null, null, null, null};
        String[] morph_tags = {"", "Arzt<+NN><Masc><Nom><Sg>", "Arzt<+NN><Masc><Dat><Sg>", "Arzt<+NN><Masc><Acc><Sg>",
                "arbeiten<+V><2><Pl><Pres><Subj>", "arbeiten<+V><2><Pl><Pres><Ind>", "arbeiten<+V><Imp><Pl>",
                "arbeiten<+V><3><Sg><Pres><Ind>", "in<+PREPART><Masc><Dat><Sg>", "in<+PREPART><Neut><Dat><Sg>",
                "kranken<V><NN><SUFF>Haus<+NN><Neut><Nom><Sg>", "kranken<V><NN><SUFF>Haus<+NN><Neut><Dat><Sg>",
                "kranken<V><NN><SUFF>Haus<+NN><Neut><Acc><Sg>", "krank<ADJ><NN><SUFF>Haus<+NN><Neut><Nom><Sg>",
                "krank<ADJ><NN><SUFF>Haus<+NN><Neut><Dat><Sg>", "krank<ADJ><NN><SUFF>Haus<+NN><Neut><Acc><Sg>",
                "Krankenhaus<+NN><Neut><Nom><Sg>", "Krankenhaus<+NN><Neut><Dat><Sg>", "Krankenhaus<+NN><Neut><Acc><Sg>", ".<+PUNCT><Norm>"};

        String[] lemmas = {"", "Arzt", "Arzt", "Arzt", "arbeiten", "arbeiten", "arbeiten", "arbeiten", "in", "in",
                "kranken<V><NN><SUFF>Haus", "kranken<V><NN><SUFF>Haus", "kranken<V><NN><SUFF>Haus", "krank<ADJ><NN><SUFF>Haus",
                "krank<ADJ><NN><SUFF>Haus", "krank<ADJ><NN><SUFF>Haus", "Krankenhaus", "Krankenhaus", "Krankenhaus", "."};

        String[] pos_tags = {"", "<+NN>", "<+NN>", "<+NN>", "<+V>", "<+V>", "<+V>", "<+V>", "<+PREPART>", "<+PREPART>",
                "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+PUNCT>"};
        Container container = Serializer.parse(data, DataContainer.class).getPayload();
        assertEquals("Text not set correctly", "Der Arzt arbeitet im Krankenhaus .", container.getText());

        // Now, see all annotations in current view is correct
        List<View> views = container.getViews();
        if (views.size() != 1) {
            fail(String.format("Expected 1 view. Found: %d", views.size()));
        }
        View view = views.get(0);
        assertTrue("View does not contain tokens", view.contains(Discriminators.Uri.TOKEN));
        List<Annotation> annotations = view.getAnnotations();
        if (annotations.size() != 20) {
            fail(String.format("Expected 20 annotations. Found %d", annotations.size()));
        }

        for (int i = 0; i < annotations.size(); i++){
            Annotation token = annotations.get(i);
            assertEquals("Token " + ids[i] + ": wrong start", starts[i], token.getStart().longValue());
            assertEquals("Token " + ids[i] + ": wrong end", ends[i], token.getEnd().longValue());
            assertEquals("Token " + ids[i] + ": wrong word", words[i], token.getFeature(Features.Token.WORD));
            assertEquals("Token " + ids[i] + ": wrong case", cases[i], token.getFeature("case"));
            assertEquals("Token " + ids[i] + ": wrong lemma", lemmas[i], token.getFeature(Features.Token.LEMMA));
            assertEquals("Token " + ids[i] + ": wrong pos tag", pos_tags[i], token.getFeature(Features.Token.POS));
            assertEquals("Token " + ids[i] + ": wrong morph tag", morph_tags[i], token.getFeature("morph_tag"));
            assertEquals("Token " + ids[i] + ": wrong number",  numbers[i], token.getFeature("number"));
            assertEquals("Token " + ids[i] + ": wrong gender", genders[i], token.getFeature("gender"));
            assertEquals("Token " + ids[i] + ": wrong mood", moods[i], token.getFeature("mood"));
            assertEquals("Token " + ids[i] + ": wrong person", persons[i], token.getFeature("person"));
            assertEquals("Token " + ids[i] + ": wrong tense", tenses[i], token.getFeature("tense"));
            assertEquals("Token " + ids[i] + ": wrong id", "tok" + ids[i], token.getId());
        }
    }

    @Test
    public void testGermanZmorgeOrig()
        throws Exception
    {
        String input_string = "de; zmorge-orig-ca; Der Arzt arbeitet im Krankenhaus .";
        String data = service.execute(input_string);
        long[] starts = {0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 4L, 4L, 4L, 9L, 9L, 9L, 9L, 18L, 18L, 21L, 21L, 21L, 21L,
                21L, 21L, 21L, 21L, 21L, 21L, 21L, 21L, 33L, 33L, 33L, 33L, 33L};
        long[] ends = {3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 8L, 8L, 8L, 17L, 17L, 17L, 17L, 20L, 20L, 32L, 32L, 32L, 32L,
                32L, 32L, 32L, 32L, 32L, 32L, 32L, 32L, 34L, 34L, 34L, 34L, 34L};
        String[] cases = {"Dat", "Nom", "Dat", "Nom", "Gen", "Dat", "Gen", "Nom", "Acc", "Dat", "Nom", null,
                null, null, null, "Dat", "Dat", "Acc", "Dat", "Nom", "Acc", "Dat", "Nom", "Acc", "Dat", "Nom", "Acc", "Dat",
                "Nom", "Acc", "Dat", "Gen", "Nom", null};
        String[] words = {"Der", "Der", "Der", "Der", "Der", "Der", "Der", "Der", "Arzt", "Arzt", "Arzt",
                "arbeitet", "arbeitet", "arbeitet", "arbeitet", "im", "im", "Krankenhaus", "Krankenhaus", "Krankenhaus",
                "Krankenhaus", "Krankenhaus", "Krankenhaus", "Krankenhaus", "Krankenhaus", "Krankenhaus", "Krankenhaus",
                "Krankenhaus", "Krankenhaus", ".", ".", ".", ".", "."};
        String[] ids = {"0_0", "0_1", "0_2", "0_3", "0_4", "0_5", "0_6", "0_7", "1_0", "1_1", "1_2", "2_0",
                "2_1", "2_2", "2_3", "3_0", "3_1", "4_0", "4_1", "4_2", "4_3", "4_4", "4_5", "4_6", "4_7", "4_8", "4_9", "4_10",
                "4_11", "5_0", "5_1", "5_2", "5_3", "5_4"};
        String[] genders = {"Fem", "Masc", "Fem", "Masc", null, "Fem", "Fem", "Masc", "Masc", "Masc", "Masc",
                null, null, null, null, "Neut", "Masc", "Neut", "Neut", "Neut", "Neut", "Neut", "Neut", "Neut", "Neut", "Neut",
                "Neut", "Neut", "Neut", "Fem", "Fem", "Fem", "Fem", null};
        String[] moods = {null, null, null, null, null, null, null, null, null, null, null, "Sub", "Imp", "Ind",
                "Ind", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};
        String[] numbers = {"Sing", "Sing", "Sing", "Sing", "Plur", "Sing", "Sing", "Sing", "Sing", "Sing",
                "Sing", "Plur", "Plur", "Sing", "Plur", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing",
                "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", null};
        String[] persons = {null, null, null, null, null, null, null, null, null, null, null, "2", null, "3",
                "2", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};
        String[] tenses = {null, null, null, null, null, null, null, null, null, null, null, "Pres", null, "Pres",
                "Pres", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};

        String[] morph_tags = {"<CAP>die<+REL><Subst><Fem><Dat><Sg><St>", "<CAP>die<+REL><Subst><Masc><Nom><Sg><St>",
                "<CAP>die<+DEM><Subst><Fem><Dat><Sg><St>", "<CAP>die<+DEM><Subst><Masc><Nom><Sg><St>", "<CAP>die<+ART><Def><NoGend><Gen><Pl><St>",
                "<CAP>die<+ART><Def><Fem><Dat><Sg><St>", "<CAP>die<+ART><Def><Fem><Gen><Sg><St>", "<CAP>die<+ART><Def><Masc><Nom><Sg><St>",
                "Arzt<+NN><Masc><Acc><Sg>", "Arzt<+NN><Masc><Dat><Sg>", "Arzt<+NN><Masc><Nom><Sg>",
                "arbeiten<+V><2><Pl><Pres><Subj>", "arbeiten<+V><Imp><Pl>", "arbeiten<+V><3><Sg><Pres><Ind>", "arbeiten<+V><2><Pl><Pres><Ind>",
                "in<+PREPART><Neut><Dat><Sg>", "in<+PREPART><Masc><Dat><Sg>", "krank<ADJ><NN><SUFF>Haus<+NN><Neut><Acc><Sg>",
                "krank<ADJ><NN><SUFF>Haus<+NN><Neut><Dat><Sg>", "krank<ADJ><NN><SUFF>Haus<+NN><Neut><Nom><Sg>",
                "kranken<V><NN><SUFF>Haus<+NN><Neut><Acc><Sg>",
                "kranken<V><NN><SUFF>Haus<+NN><Neut><Dat><Sg>", "kranken<V><NN><SUFF>Haus<+NN><Neut><Nom><Sg>",
                "Kran<NN>Ken<NN>Haus<+NN><Neut><Acc><Sg>", "Kran<NN>Ken<NN>Haus<+NN><Neut><Dat><Sg>",
                "Kran<NN>Ken<NN>Haus<+NN><Neut><Nom><Sg>", "Krankenhaus<+NN><Neut><Acc><Sg>", "Krankenhaus<+NN><Neut><Dat><Sg>",
                "Krankenhaus<+NN><Neut><Nom><Sg>", ".<^ABBR><+NN><Fem><Acc><Sg>", ".<^ABBR><+NN><Fem><Dat><Sg>",
                ".<^ABBR><+NN><Fem><Gen><Sg>", ".<^ABBR><+NN><Fem><Nom><Sg>", ".<+PUNCT><Norm>"};

        String[] pos_tags = {"<+REL>", "<+REL>", "<+DEM>", "<+DEM>", "<+ART>", "<+ART>", "<+ART>", "<+ART>",
                "<+NN>", "<+NN>", "<+NN>", "<+V>", "<+V>", "<+V>", "<+V>", "<+PREPART>", "<+PREPART>", "<+NN>",
                "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>",
                "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+PUNCT>"};

        String[] lemmas = {"<CAP>die", "<CAP>die", "<CAP>die", "<CAP>die", "<CAP>die", "<CAP>die", "<CAP>die", "<CAP>die",
                "Arzt", "Arzt", "Arzt", "arbeiten", "arbeiten", "arbeiten", "arbeiten", "in", "in", "krank<ADJ><NN><SUFF>Haus",
                "krank<ADJ><NN><SUFF>Haus", "krank<ADJ><NN><SUFF>Haus", "kranken<V><NN><SUFF>Haus", "kranken<V><NN><SUFF>Haus",
                "kranken<V><NN><SUFF>Haus", "Kran<NN>Ken<NN>Haus", "Kran<NN>Ken<NN>Haus", "Kran<NN>Ken<NN>Haus",
                "Krankenhaus", "Krankenhaus", "Krankenhaus", ".<^ABBR>", ".<^ABBR>", ".<^ABBR>", ".<^ABBR>", "."};

        String[] defs = {null, null, null, null, "Def", "Def", "Def", "Def", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};

        String[] pron_types = {"Rel", "Rel", null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};

        Container container = Serializer.parse(data, DataContainer.class).getPayload();
        assertEquals("Text not set correctly", "Der Arzt arbeitet im Krankenhaus .", container.getText());

        // Now, see all annotations in current view is correct
        List<View> views = container.getViews();
        if (views.size() != 1) {
            fail(String.format("Expected 1 view. Found: %d", views.size()));
        }
        View view = views.get(0);
        assertTrue("View does not contain tokens", view.contains(Discriminators.Uri.TOKEN));
        List<Annotation> annotations = view.getAnnotations();
        if (annotations.size() != 34) {
            fail(String.format("Expected 34 annotations. Found %d", annotations.size()));
        }

        for (int i = 0; i < annotations.size(); i++){
            Annotation token = annotations.get(i);
            assertEquals("Token " + ids[i] + ": wrong start", starts[i], token.getStart().longValue());
            assertEquals("Token " + ids[i] + ": wrong end", ends[i], token.getEnd().longValue());
            assertEquals("Token " + ids[i] + ": wrong word", words[i], token.getFeature(Features.Token.WORD));
            assertEquals("Token " + ids[i] + ": wrong case", cases[i], token.getFeature("case"));
            assertEquals("Token " + ids[i] + ": wrong lemma", lemmas[i], token.getFeature(Features.Token.LEMMA));
            assertEquals("Token " + ids[i] + ": wrong pos tag" + token, pos_tags[i], token.getFeature(Features.Token.POS));
            assertEquals("Token " + ids[i] + ": wrong morph tag", morph_tags[i], token.getFeature("morph_tag"));
            assertEquals("Token " + ids[i] + ": wrong definiteness", defs[i], token.getFeature("definiteness"));
            assertEquals("Token " + ids[i] + ": wrong number",  numbers[i], token.getFeature("number"));
            assertEquals("Token " + ids[i] + ": wrong gender", genders[i], token.getFeature("gender"));
            assertEquals("Token " + ids[i] + ": wrong mood", moods[i], token.getFeature("mood"));
            assertEquals("Token " + ids[i] + ": wrong person", persons[i], token.getFeature("person"));
            assertEquals("Token " + ids[i] + ": wrong pronoun type", pron_types[i], token.getFeature("pron_type"));
            assertEquals("Token " + ids[i] + ": wrong tense", tenses[i], token.getFeature("tense"));
            assertEquals("Token " + ids[i] + ": wrong id", "tok" + ids[i], token.getId());
        }
    }

    @Test
    public void testGermanZmorgeNewLemma()
        throws Exception
    {
        String input_string = "de; zmorge-newlemma-ca; Der Arzt arbeitet im Krankenhaus .";
        String data = service.execute(input_string);
        long[] starts = {0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 4L, 4L, 4L, 9L, 9L, 9L, 9L, 18L, 18L, 21L, 21L, 21L, 21L,
                21L, 21L, 21L, 21L, 21L, 21L, 21L, 21L, 33L, 33L, 33L, 33L, 33L};
        long[] ends = {3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 8L, 8L, 8L, 17L, 17L, 17L, 17L, 20L, 20L, 32L, 32L, 32L, 32L,
                32L, 32L, 32L, 32L, 32L, 32L, 32L, 32L, 34L, 34L, 34L, 34L, 34L};
        String[] cases = {"Dat", "Nom", "Dat", "Nom", "Gen", "Dat", "Gen", "Nom", "Acc", "Dat", "Nom", null,
                null, null, null, "Dat", "Dat", "Acc", "Dat", "Nom", "Acc", "Dat", "Nom", "Acc", "Dat", "Nom", "Acc", "Dat",
                "Nom", "Acc", "Dat", "Gen", "Nom", null};
        String[] words = {"Der", "Der", "Der", "Der", "Der", "Der", "Der", "Der", "Arzt", "Arzt", "Arzt",
                "arbeitet", "arbeitet", "arbeitet", "arbeitet", "im", "im", "Krankenhaus", "Krankenhaus", "Krankenhaus",
                "Krankenhaus", "Krankenhaus", "Krankenhaus", "Krankenhaus", "Krankenhaus", "Krankenhaus", "Krankenhaus",
                "Krankenhaus", "Krankenhaus", ".", ".", ".", ".", "."};
        String[] ids = {"0_0", "0_1", "0_2", "0_3", "0_4", "0_5", "0_6", "0_7", "1_0", "1_1", "1_2", "2_0",
                "2_1", "2_2", "2_3", "3_0", "3_1", "4_0", "4_1", "4_2", "4_3", "4_4", "4_5", "4_6", "4_7", "4_8", "4_9", "4_10",
                "4_11", "5_0", "5_1", "5_2", "5_3", "5_4"};
        String[] genders = {"Fem", "Masc", "Fem", "Masc", null, "Fem", "Fem", "Masc", "Masc", "Masc", "Masc",
                null, null, null, null, "Neut", "Masc", "Neut", "Neut", "Neut", "Neut", "Neut", "Neut", "Neut", "Neut", "Neut",
                "Neut", "Neut", "Neut", "Fem", "Fem", "Fem", "Fem", null};
        String[] moods = {null, null, null, null, null, null, null, null, null, null, null, "Sub", "Imp", "Ind",
                "Ind", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};
        String[] numbers = {"Sing", "Sing", "Sing", "Sing", "Plur", "Sing", "Sing", "Sing", "Sing", "Sing",
                "Sing", "Plur", "Plur", "Sing", "Plur", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing",
                "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", "Sing", null};
        String[] persons = {null, null, null, null, null, null, null, null, null, null, null, "2", null, "3",
                "2", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};
        String[] tenses = {null, null, null, null, null, null, null, null, null, null, null, "Pres", null, "Pres",
                "Pres", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};

        String[] morph_tags = {"<CAP>die<+REL><Subst><Fem><Dat><Sg><St>", "<CAP>die<+REL><Subst><Masc><Nom><Sg><St>",
                "<CAP>die<+DEM><Subst><Fem><Dat><Sg><St>", "<CAP>die<+DEM><Subst><Masc><Nom><Sg><St>", "<CAP>die<+ART><Def><NoGend><Gen><Pl><St>",
                "<CAP>die<+ART><Def><Fem><Dat><Sg><St>", "<CAP>die<+ART><Def><Fem><Gen><Sg><St>", "<CAP>die<+ART><Def><Masc><Nom><Sg><St>",
                "Arzt<+NN><Masc><Acc><Sg>", "Arzt<+NN><Masc><Dat><Sg>", "Arzt<+NN><Masc><Nom><Sg>",
                "arbeit<~>en<+V><2><Pl><Pres><Subj>", "arbeit<~>en<+V><Imp><Pl>", "arbeit<~>en<+V><3><Sg><Pres><Ind>", "arbeit<~>en<+V><2><Pl><Pres><Ind>",
                "in<+PREPART><Neut><Dat><Sg>", "in<+PREPART><Masc><Dat><Sg>", "Kran<#>ken<#>haus<+NN><Neut><Acc><Sg>",
                "Kran<#>ken<#>haus<+NN><Neut><Dat><Sg>", "Kran<#>ken<#>haus<+NN><Neut><Nom><Sg>",
                "Krank<~>en<#>haus<+NN><Neut><Acc><Sg>", "Krank<~>en<#>haus<+NN><Neut><Dat><Sg>", "Krank<~>en<#>haus<+NN><Neut><Nom><Sg>",
                "Krank<~>en<#>haus<+NN><Neut><Acc><Sg>", "Krank<~>en<#>haus<+NN><Neut><Dat><Sg>",
                "Krank<~>en<#>haus<+NN><Neut><Nom><Sg>", "Krankenhaus<+NN><Neut><Acc><Sg>", "Krankenhaus<+NN><Neut><Dat><Sg>",
                "Krankenhaus<+NN><Neut><Nom><Sg>", ".<+NN><Fem><Acc><Sg>", ".<+NN><Fem><Dat><Sg>",
                ".<+NN><Fem><Gen><Sg>", ".<+NN><Fem><Nom><Sg>", ".<+PUNCT><Norm>"};

        String[] pos_tags = {"<+REL>", "<+REL>", "<+DEM>", "<+DEM>", "<+ART>", "<+ART>", "<+ART>", "<+ART>",
                "<+NN>", "<+NN>", "<+NN>", "<+V>", "<+V>", "<+V>", "<+V>", "<+PREPART>", "<+PREPART>", "<+NN>",
                "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+NN>",
                "<+NN>", "<+NN>", "<+NN>", "<+NN>", "<+PUNCT>"};

        String[] lemmas = {"<CAP>die", "<CAP>die", "<CAP>die", "<CAP>die", "<CAP>die", "<CAP>die", "<CAP>die", "<CAP>die",
                "Arzt", "Arzt", "Arzt", "arbeit<~>en", "arbeit<~>en", "arbeit<~>en", "arbeit<~>en", "in", "in", "Kran<#>ken<#>haus",
                "Kran<#>ken<#>haus", "Kran<#>ken<#>haus", "Krank<~>en<#>haus", "Krank<~>en<#>haus",
                "Krank<~>en<#>haus", "Krank<~>en<#>haus", "Krank<~>en<#>haus", "Krank<~>en<#>haus",
                "Krankenhaus", "Krankenhaus", "Krankenhaus", ".", ".", ".", ".", "."};

        String[] defs = {null, null, null, null, "Def", "Def", "Def", "Def", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};

        String[] pron_types = {"Rel", "Rel", null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};

        Container container = Serializer.parse(data, DataContainer.class).getPayload();
        assertEquals("Text not set correctly", "Der Arzt arbeitet im Krankenhaus .", container.getText());

        // Now, see all annotations in current view is correct
        List<View> views = container.getViews();
        if (views.size() != 1) {
            fail(String.format("Expected 1 view. Found: %d", views.size()));
        }
        View view = views.get(0);
        assertTrue("View does not contain tokens", view.contains(Discriminators.Uri.TOKEN));
        List<Annotation> annotations = view.getAnnotations();
        if (annotations.size() != 34) {
            fail(String.format("Expected 34 annotations. Found %d", annotations.size()));
        }

        for (int i = 0; i < annotations.size(); i++){
            Annotation token = annotations.get(i);
            assertEquals("Token " + ids[i] + ": wrong start", starts[i], token.getStart().longValue());
            assertEquals("Token " + ids[i] + ": wrong end", ends[i], token.getEnd().longValue());
            assertEquals("Token " + ids[i] + ": wrong word", words[i], token.getFeature(Features.Token.WORD));
            assertEquals("Token " + ids[i] + ": wrong case", cases[i], token.getFeature("case"));
            assertEquals("Token " + ids[i] + ": wrong lemma", lemmas[i], token.getFeature(Features.Token.LEMMA));
            assertEquals("Token " + ids[i] + ": wrong pos tag" + token, pos_tags[i], token.getFeature(Features.Token.POS));
            assertEquals("Token " + ids[i] + ": wrong morph tag", morph_tags[i], token.getFeature("morph_tag"));
            assertEquals("Token " + ids[i] + ": wrong definiteness", defs[i], token.getFeature("definiteness"));
            assertEquals("Token " + ids[i] + ": wrong number",  numbers[i], token.getFeature("number"));
            assertEquals("Token " + ids[i] + ": wrong gender", genders[i], token.getFeature("gender"));
            assertEquals("Token " + ids[i] + ": wrong mood", moods[i], token.getFeature("mood"));
            assertEquals("Token " + ids[i] + ": wrong person", persons[i], token.getFeature("person"));
            assertEquals("Token " + ids[i] + ": wrong pronoun type", pron_types[i], token.getFeature("pron_type"));
            assertEquals("Token " + ids[i] + ": wrong tense", tenses[i], token.getFeature("tense"));
            assertEquals("Token " + ids[i] + ": wrong id", "tok" + ids[i], token.getId());
        }
    }

    @Test
    public void testItalian()
            throws Exception
    {
        String input_string = "it; pippi-ca; Il medico che lavora in ospedale .";
        String data = service.execute(input_string);

        Container container = Serializer.parse(data, DataContainer.class).getPayload();
        assertEquals("Text not set correctly", "Il medico che lavora in ospedale .", container.getText());

        // Now, see all annotations in current view is correct

        long[] starts = {0L, 3L, 3L, 3L, 10L, 10L, 10L, 10L, 10L, 10L, 14L, 14L, 21L, 24L, 33L};
        long[] ends = {2L, 9L, 9L, 9L, 13L, 13L, 13L, 13L, 13L, 13L, 20L, 20L, 23L, 32L, 34L};
        String[] words = {"Il", "medico", "medico", "medico", "che", "che", "che", "che", "che", "che", "lavora", "lavora",
        "in", "ospedale", "."};
        String[] ids = {"0_0", "1_0", "1_1", "1_2", "2_0", "2_1", "2_2", "2_3", "2_4", "2_5", "3_0", "3_1", "4_0", "5_0", "6_0"};
        String[] genders = {null, "Masc", null, null, null, "Fem", "Fem", "Masc", "Masc", null, null, null, null, null, null};
        String[] moods = {null, null, null, "Ind", null, null, null, null, null, null, null, "Ind", null, null, null};
        String[] numbers = {null, "Sing", "Sing", "Sing", null, "Plur", "Sing", "Plur", "Sing", null, "Sing", "Sing", null, "Sing", null};
        String[] persons = {null, null, null, "1", null, null, null, null, null, null, "2", "3", null, null, null};
        String[] tenses = {null, null, null, "Pres", null, null, null, null, null, null, "Pres", "Pres", null, null, null};
        String[] morph_tags = {"", "medico<ADJ><pos><m><s>", "medico<NOUN><M><s>", "medicare<VER><ind><pres><1><s>",
        "che<CON>", "che<DET><WH><f><p>", "che<DET><WH><f><s>", "che<DET><WH><m><p>", "che<DET><WH><m><s>", "che<WH><CHE>",
        "lavorare<VER><impr><pres><2><s>", "lavorare<VER><ind><pres><3><s>", "in<PRE>", "ospedale<NOUN><M><s>", ".<SENT>"};
        String[] pos_tags = {"", "<ADJ>", "<NOUN>", "<VER>", "<CON>", "<DET>", "<DET>", "<DET>", "<DET>", "<WH>",
        "<VER>", "<VER>", "<PRE>", "<NOUN>", "<SENT>"};
        String[] lemmas = {"", "medico", "medico", "medicare", "che", "che", "che", "che", "che", "che", "lavorare", "lavorare", "in", "ospedale", "."};
        String[] cases = {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};


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
            assertEquals("Token " + ids[i] + ": wrong start", starts[i], token.getStart().longValue());
            assertEquals("Token " + ids[i] + ": wrong end", ends[i], token.getEnd().longValue());
            assertEquals("Token " + ids[i] + ": wrong word", words[i], token.getFeature(Features.Token.WORD));
            assertEquals("Token " + ids[i] + ": wrong case", cases[i], token.getFeature("case"));
            assertEquals("Token " + ids[i] + ": wrong lemma", lemmas[i], token.getFeature(Features.Token.LEMMA));
            assertEquals("Token " + ids[i] + ": wrong pos tag" + token, pos_tags[i], token.getFeature(Features.Token.POS));
            assertEquals("Token " + ids[i] + ": wrong morph tag", morph_tags[i], token.getFeature("morph_tag"));
            assertEquals("Token " + ids[i] + ": wrong number",  numbers[i], token.getFeature("number"));
            assertEquals("Token " + ids[i] + ": wrong gender", genders[i], token.getFeature("gender"));
            assertEquals("Token " + ids[i] + ": wrong mood", moods[i], token.getFeature("mood"));
            assertEquals("Token " + ids[i] + ": wrong person", persons[i], token.getFeature("person"));
            assertEquals("Token " + ids[i] + ": wrong tense", tenses[i], token.getFeature("tense"));
            assertEquals("Token " + ids[i] + ": wrong id", "tok" + ids[i], token.getId());
        }
    }
}

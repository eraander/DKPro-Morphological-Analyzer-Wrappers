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
import static org.junit.Assert.assertEquals;

public class RfTaggerLappsTest
{
    @Rule
    public DkproTestContext testContext = new DkproTestContext();

    protected WebService service;

    // initiate the service before each test
    @Before
    public void setUp() throws IOException, ResourceInitializationException, CASException, UIMAException {
        service = new org.lappsgrid.rftagger_lapps.RfTaggerLapps();
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
        assertEquals("Name is not correct", org.lappsgrid.rftagger_lapps.RfTaggerLapps.class.getName(), metadata.getName());
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
    public void testCzech()
            throws Exception
    {
        String text = "Vzal mi pochodeň a porazit je na medvěda tváři .";
        String input = "cz;  null; " + text;
        String data = service.execute(input);

        long[] starts = {0L, 5L, 8L, 17L, 19L, 27L, 30L, 33L, 41L, 47L};
        long[] ends = {4L, 7L, 16L, 18L, 26L, 29L, 32L, 40L, 46L, 48L};
        String[] words = { "Vzal", "mi", "pochodeň", "a", "porazit", "je", "na", "medvěda",
                "tváři", "." };

        String[] posOrig = { "V", "P", "N", "J", "V", "P", "R", "A", "N", "Z" };

        String[] posMapped = { "POS_VERB", "POS_PRON", "POS_NOUN", "POS_CONJ", "POS_VERB",
                "POS_PRON", "POS_ADP", "POS_ADJ", "POS_NOUN", "POS_PUNCT" };

        String[] morph_tags = {"V.p.Y.S.-.-.-.X.R.-.A.A.-.-.-", "P.H.-.S.3.-.-.1.-.-.-.-.-.-.-",
        "N.N.F.S.4.-.-.-.-.-.A.-.-.-.-", "J.^.-.-.-.-.-.-.-.-.-.-.-.-.-", "V.f.-.-.-.-.-.-.-.-.A.-.-.-.-",
        "P.P.X.P.4.-.-.3.-.-.-.-.-.-.-", "R.R.-.-.6.-.-.-.-.-.-.-.-.-.-", "A.A.F.S.6.-.-.-.-.1.A.-.-.-.-",
        "N.N.F.S.6.-.-.-.-.-.A.-.-.-.-", "Z.:.-.-.-.-.-.-.-.-.-.-.-.-.-"};

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
        if (annotations.size() != 10) {
            fail(String.format("Expected 10 annotations. Found %d", annotations.size()));
        }

        for (int i = 0; i < annotations.size(); i++) {
            Annotation token = annotations.get(i);
            assertEquals("Token " + i + ": wrong start", starts[i], token.getStart().longValue());
            assertEquals("Token " + i + ": wrong end", ends[i], token.getEnd().longValue());
            assertEquals("Token " + i + ": wrong word", words[i], token.getFeature(Features.Token.WORD));
            assertEquals("Token " + i + ": wrong pos tag", posOrig[i], token.getFeature(Features.Token.POS));
            assertEquals("Token " + i + ": wrong mapped pos", posMapped[i], token.getFeature("pos_mapped"));
            assertEquals("Token " + i + ": wrong morph tag", morph_tags[i], token.getFeature("morph_tag"));
        }


        // assertTagsetParser(MorphologicalFeatures.class, "cac", unmappedTags, jcas);
    }

    @Test
    public void testGerman()
            throws Exception
    {
        String text = "Er nahm meine Fackel und schlug sie dem Bär ins Gesicht .";
        String input = "de; null; " + text;
        String data = service.execute(input);
        long[] starts = {0L, 3L, 8L, 14L, 21L, 25L, 32L, 36L, 40L, 44L, 48L, 56L};
        long[] ends = {2L, 7L, 13L, 20L, 24L, 31L, 35L, 39L, 43L, 47L, 55L, 57L};
        String[] words = { "Er", "nahm", "meine", "Fackel", "und", "schlug", "sie", "dem", "Bär",
                "ins", "Gesicht", "." };

        String[] posOrig = { "PRO", "VFIN", "PRO", "N", "CONJ", "VFIN", "PRO", "ART", "N",
                "APPRART", "N", "SYM" };

        String[] posMapped = { "POS_PRON", "POS_VERB", "POS_PRON", "POS_NOUN", "POS_CONJ",
                "POS_VERB", "POS_PRON", "POS_DET", "POS_NOUN", "POS_ADP", "POS_NOUN", "POS_PUNCT" };

        String[] cases = {"Nom", null, "Acc", "Acc", null, null, "Acc", "Dat", "Dat", "Acc", "Acc", null};
        String[] defs = {null, null, null, null, null, null, null, "Def", null, null, null, null};
        String[] degrees = {null, null, "Pos", null, null, null, null, null, null, null, null, null};
        String[] genders = {"Masc", null, "Fem", "Fem", null, null, null, "Masc", "Masc", "Neut", "Neut", null};
        String[] moods = {null, "Ind", null, null, null, "Ind", null, null, null, null, null, null};
        String[] numbers = {"Sing", "Sing", "Sing", "Sing", null, "Sing", "Plur", "Sing", "Sing", "Sing", "Sing", null};
        String[] persons = {"3", "3", null, null, null, "3", "3", null, null, null, null, null};
        String[] possessives = {null, null, "Yes", null, null, null, null, null, null, null, null, null};
        String[] pron_types = {"Prs", null, null, null, null, null, "Prs", null, null, null, null, null};
        String[] tenses = {null, "Past", null, null, null, "Past", null, null, null, null, null, null};
        String[] morph_tags = {"PRO.Pers.Subst.3.Nom.Sg.Masc", "VFIN.Full.3.Sg.Past.Ind", "PRO.Poss.Attr.-.Acc.Sg.Fem",
                "N.Reg.Acc.Sg.Fem", "CONJ.Coord.-", "VFIN.Full.3.Sg.Past.Ind", "PRO.Pers.Subst.3.Acc.Pl.*", "ART.Def.Dat.Sg.Masc",
                "N.Reg.Dat.Sg.Masc", "APPRART.Acc.Sg.Neut", "N.Reg.Acc.Sg.Neut", "SYM.Pun.Sent"};

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
        if (annotations.size() != 12) {
            fail(String.format("Expected 12 annotations. Found %d", annotations.size()));
        }

        for (int i = 0; i < annotations.size(); i++) {
            Annotation token = annotations.get(i);
            assertEquals("Token " + i + ": wrong start", starts[i], token.getStart().longValue());
            assertEquals("Token " + i + ": wrong end", ends[i], token.getEnd().longValue());
            assertEquals("Token " + i + ": wrong word", words[i], token.getFeature(Features.Token.WORD));
            assertEquals("Token " + i + ": wrong pos tag", posOrig[i], token.getFeature(Features.Token.POS));
            assertEquals("Token " + i + ": wrong mapped pos", posMapped[i], token.getFeature("pos_mapped"));
            assertEquals("Token " + i + ": wrong morph tag", morph_tags[i], token.getFeature("morph_tag"));
            assertEquals("Token " + i + ": wrong case", cases[i], token.getFeature("case"));
            assertEquals("Token " + i + ": wrong degree", degrees[i], token.getFeature("degree"));
            assertEquals("Token " + i + ": wrong gender", genders[i], token.getFeature("gender"));
            assertEquals("Token " + i + ": wrong mood", moods[i], token.getFeature("mood"));
            assertEquals("Token " + i + ": wrong definiteness", defs[i], token.getFeature("definiteness"));
            assertEquals("Token " + i + ": wrong number",  numbers[i], token.getFeature("number"));
            assertEquals("Token " + i + ": wrong person", persons[i], token.getFeature("person"));
            assertEquals("Token " + i + ": wrong possessive value", possessives[i], token.getFeature("possessive"));
            assertEquals("Token " + i + ": wrong pronoun type", pron_types[i], token.getFeature("pron_type"));
            assertEquals("Token " + i + ": wrong tense", tenses[i], token.getFeature("tense"));
        }
    }

    @Test
    public void testHungarian()
        throws Exception
    {
        String text = "Elvette a fáklyát , és megverte őket, hogy a medve arcára .";
        String input = "hu; null; " + text;
        String data = service.execute(input);
        long[] starts = {0L, 8L, 10L, 18L, 20L, 23L, 32L, 38L, 43L, 45L, 51L, 58L};
        long[] ends = {7L, 9L, 17L, 19L, 22L, 31L, 37L, 42L, 44L, 50L, 57L, 59L};
        String[] words = { "Elvette", "a", "fáklyát", ",", "és", "megverte", "őket,", "hogy", "a",
                "medve", "arcára", "." };

        String[] posOrig = { "V", "T", "N", "IP", "C", "V", "X", "C", "T", "N", "N", "IP" };

        String[] posMapped = { "POS", "POS", "POS", "POS", "POS", "POS", "POS", "POS", "POS",
                "POS", "POS", "POS" };

        String[] morph_tags = {"V.m.i.s.3.s", "T.f", "N.c.s.a", "IP.comma.-", "C.c.s.p", "V.m.i.s.3.s", "X", "C.s.s.p",
                "T.f", "N.c.s.n", "N.c.s.s", "IP.sent.period"};

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
        if (annotations.size() != 12) {
            fail(String.format("Expected 12 annotations. Found %d", annotations.size()));
        }

        for (int i = 0; i < annotations.size(); i++) {
            Annotation token = annotations.get(i);
            assertEquals("Token " + i + ": wrong start", starts[i], token.getStart().longValue());
            assertEquals("Token " + i + ": wrong end", ends[i], token.getEnd().longValue());
            assertEquals("Token " + i + ": wrong word", words[i], token.getFeature(Features.Token.WORD));
            assertEquals("Token " + i + ": wrong pos tag", posOrig[i], token.getFeature(Features.Token.POS));
            assertEquals("Token " + i + ": wrong mapped pos", posMapped[i], token.getFeature("pos_mapped"));
            assertEquals("Token " + i + ": wrong morph tag", morph_tags[i], token.getFeature("morph_tag"));
        }
    }

    @Test
    public void testRussian()
        throws Exception
    {
        String text = "Он взял свой факел и избили их в лицо медведя .";
        String input = "ru; null; " + text;
        String data = service.execute(input);
        long[] starts = {0L, 3L, 8L, 13L, 19L, 21L, 28L, 31L, 33L, 38L, 46L};
        long[] ends = {2L, 7L, 12L, 18L, 20L, 27L, 30L, 32L, 37L, 45L, 47L};

        String[] words = { "Он", "взял", "свой", "факел", "и", "избили", "их", "в", "лицо",
                "медведя", "." };

        String[] posOrig = { "P", "V", "P", "N", "C", "V", "P", "S", "N", "N", "SENT" };

        String[] posMapped = { "POS", "POS", "POS", "POS", "POS", "POS", "POS", "POS", "POS",
                "POS", "POS" };

        String[] morph_tags = {"P.-.3.m.s.n.n", "V.m.i.s.-.s.m.a.-.p.-", "P.-.-.m.s.a.a", "N.c.m.s.n.n.-",
        "C", "V.m.i.s.-.p.-.a.-.p.-", "P.-.3.-.p.a.n", "S.p.-.a", "N.c.n.s.a.n.-", "N.c.m.s.g.y.-", "SENT"};

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
        if (annotations.size() != 11) {
            fail(String.format("Expected 11 annotations. Found %d", annotations.size()));
        }

        for (int i = 0; i < annotations.size(); i++) {
            Annotation token = annotations.get(i);
            assertEquals("Token " + i + ": wrong start", starts[i], token.getStart().longValue());
            assertEquals("Token " + i + ": wrong end", ends[i], token.getEnd().longValue());
            assertEquals("Token " + i + ": wrong word", words[i], token.getFeature(Features.Token.WORD));
            assertEquals("Token " + i + ": wrong pos tag", posOrig[i], token.getFeature(Features.Token.POS));
            assertEquals("Token " + i + ": wrong mapped pos", posMapped[i], token.getFeature("pos_mapped"));
            assertEquals("Token " + i + ": wrong morph tag", morph_tags[i], token.getFeature("morph_tag"));
        }

    }

    @Test
    public void testSlovak()
        throws Exception
    {
        String text = "Vzal mi pochodeň a poraziť ich na medveďa tvári .";
        String input = "sk; null; " + text;
        String data = service.execute(input);

        long[] starts = {0L, 5L, 8L, 17L, 19L, 27L, 31L, 34L, 42L, 48L};
        long[] ends = {4L, 7L, 16L, 18L, 26L, 30L, 33L, 41L, 47L, 49L};

        String[] words = { "Vzal", "mi", "pochodeň", "a", "poraziť", "ich", "na", "medveďa",
                "tvári", "." };

        String[] posOrig = { "VL", "PP", "SS", "O", "VI", "PF", "E", "SS", "VK", "Z" };

        String[] posMapped = { "POS", "POS", "POS", "POS", "POS", "POS", "POS", "POS", "POS",
                "POS" };

        String[] morph_tags = {"VL.d.s.c.m.+.:-", "PP.h.s.3.-.:-", "SS.f.s.4.-.:-", "O.:-", "VI.d.+.:-", "PF.i.p.4.-.:-",
                "E.u.4.-.:-", "SS.m.s.4.-.:-", "VK.e.s.c.+.:-", "Z.:-"};

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
        if (annotations.size() != 10) {
            fail(String.format("Expected 10 annotations. Found %d", annotations.size()));
        }

        for (int i = 0; i < annotations.size(); i++) {
            Annotation token = annotations.get(i);
            assertEquals("Token " + i + ": wrong start", starts[i], token.getStart().longValue());
            assertEquals("Token " + i + ": wrong end", ends[i], token.getEnd().longValue());
            assertEquals("Token " + i + ": wrong word", words[i], token.getFeature(Features.Token.WORD));
            assertEquals("Token " + i + ": wrong pos tag", posOrig[i], token.getFeature(Features.Token.POS));
            assertEquals("Token " + i + ": wrong mapped pos", posMapped[i], token.getFeature("pos_mapped"));
            assertEquals("Token " + i + ": wrong morph tag", morph_tags[i], token.getFeature("morph_tag"));
        }
    }

    @Test
    public void testSlovene()
        throws Exception
    {
        String text = "Vzel mojo baklo in ga premagal na obrazu medveda .";
        String input = "sl; null; " + text;
        String data = service.execute(input);

        long[] starts = {0L, 5L, 10L, 16L, 19L, 22L, 31L, 34L, 41L, 49L};
        long[] ends = {4L, 9L, 15L, 18L, 21L, 30L, 33L, 40L, 48L, 50L};

        String[] words = { "Vzel", "mojo", "baklo", "in", "ga", "premagal", "na", "obrazu",
                "medveda", "." };

        String[] posOrig = { "V", "P", "N", "C", "P", "V", "S", "N", "N", "Z" };

        String[] posMapped = { "POS", "POS", "POS", "POS", "POS", "POS", "POS", "POS", "POS",
                "POS" };

        String[] morph_tags = {"V.m.e.p.-.s.m", "P.s.1.f.s.a", "N.c.f.s.a", "C.c", "P.p.3.m.s.a", "V.m.e.p.-.s.m",
                "S.l", "N.c.m.s.l", "N.c.m.s.g", "Z.p.-"};

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
        if (annotations.size() != 10) {
            fail(String.format("Expected 10 annotations. Found %d", annotations.size()));
        }

        for (int i = 0; i < annotations.size(); i++) {
            Annotation token = annotations.get(i);
            assertEquals("Token " + i + ": wrong start", starts[i], token.getStart().longValue());
            assertEquals("Token " + i + ": wrong end", ends[i], token.getEnd().longValue());
            assertEquals("Token " + i + ": wrong word", words[i], token.getFeature(Features.Token.WORD));
            assertEquals("Token " + i + ": wrong pos tag", posOrig[i], token.getFeature(Features.Token.POS));
            assertEquals("Token " + i + ": wrong mapped pos", posMapped[i], token.getFeature("pos_mapped"));
            assertEquals("Token " + i + ": wrong morph tag", morph_tags[i], token.getFeature("morph_tag"));
        }
    }
}

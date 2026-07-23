package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * little unit tests for the json helpers that live inside Main
 * these dont touch the database so they are quick and safe to run anywhere
 * mostly im just making sure the hand written parser doesnt fall over on weird input
 */
class MainHelpersTest {

    /**
     * the happy path just pull a normal string field out of a tiny json body
     */
    @Test
    void getValuePullsOutAStringField() {
        String body = "{\"username\":\"support\",\"password\":\"support123\"}";
        assertEquals("support", Main.getValue(body, "username"));
        assertEquals("support123", Main.getValue(body, "password"));
    }

    /**
     * numbers arent quoted in our json so getValue should still grab the raw digits
     * we hand that string to getInt afterwards anyway
     */
    @Test
    void getValueAlsoGrabsNumbers() {
        String body = "{\"currentUserId\":7,\"assigneeId\":0}";
        assertEquals("7", Main.getValue(body, "currentUserId"));
        assertEquals("0", Main.getValue(body, "assigneeId"));
    }

    /**
     * if the key isnt in the body we want an empty string back not a crash
     */
    @Test
    void missingKeyGivesEmptyString() {
        String body = "{\"title\":\"printer is broken\"}";
        assertEquals("", Main.getValue(body, "description"));
    }

    /**
     * people leave a space after the colon sometimes so check that case too
     */
    @Test
    void handlesSpaceAfterColon() {
        String body = "{\"title\": \"vpn is down\"}";
        assertEquals("vpn is down", Main.getValue(body, "title"));
    }

    /**
     * an escaped quote inside the value used to break the old version of this
     * so im keeping this one around as a regression check
     */
    @Test
    void readsValueWithEscapedQuote() {
        String body = "{\"text\":\"he said \\\"hi\\\" to me\"}";
        assertEquals("he said \"hi\" to me", Main.getValue(body, "text"));
    }

    /**
     * getInt should turn text into a number and quietly give back 0 when its junk
     */
    @Test
    void getIntParsesOrFallsBackToZero() {
        assertEquals(42, Main.getInt("42"));
        assertEquals(0, Main.getInt("not a number"));
        assertEquals(0, Main.getInt(""));
    }

    /**
     * esc has to escape the characters that would otherwise break our hand built json
     * we also strip carriage returns becuase windows line endings kept sneaking in
     */
    @Test
    void escEscapesTheTrickyCharacters() {
        assertEquals("line1\\nline2", Main.esc("line1\nline2"));
        assertEquals("a \\\"quote\\\" here", Main.esc("a \"quote\" here"));
        assertEquals("back\\\\slash", Main.esc("back\\slash"));
        // the carriage return should just disapear
        assertEquals("noCR", Main.esc("noCR\r"));
    }

    /**
     * null in null out we really dont want a null pointer when a field is empty
     */
    @Test
    void escHandlesNull() {
        assertEquals("", Main.esc(null));
    }
}

package dev.gustavorosa.cpsystem.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NameUtilsTest {

    @Test
    void toTitleCase_shouldFormatNormalName() {
        assertEquals("Gustavo Rosa", NameUtils.toTitleCase("GUSTAVO ROSA"));
        assertEquals("Gustavo Rosa", NameUtils.toTitleCase("gustavo rosa"));
        assertEquals("Gustavo Rosa", NameUtils.toTitleCase("gUsTaVo rOsA"));
    }

    @Test
    void toTitleCase_shouldHandleMultipleSpaces() {
        assertEquals("Gustavo Rosa", NameUtils.toTitleCase("  gustavo    rosa  "));
    }

    @Test
    void toTitleCase_shouldReturnNullForNullInput() {
        assertNull(NameUtils.toTitleCase(null));
    }

    @Test
    void toTitleCase_shouldReturnEmptyForBlankInput() {
        assertEquals("", NameUtils.toTitleCase(""));
        assertEquals("", NameUtils.toTitleCase("   "));
    }

    @Test
    void toTitleCase_shouldHandleSingleWord() {
        assertEquals("Gustavo", NameUtils.toTitleCase("gustavo"));
    }

    @Test
    void toTitleCase_shouldKeepConnectivesLowercase() {
        assertEquals("Maria da Silva e Souza", NameUtils.toTitleCase("MARIA DA SILVA E SOUZA"));
        assertEquals("João de Oliveira dos Santos", NameUtils.toTitleCase("joão DE oliveira DOS santos"));
        assertEquals("Ana do Carmo das Neves", NameUtils.toTitleCase("ana do carmo das neves"));
    }

    @Test
    void toTitleCase_shouldCapitalizeConnectiveAsFirstWord() {
        assertEquals("De Souza", NameUtils.toTitleCase("de souza"));
    }

    @Test
    void toTitleCase_shouldHandleAccentedCharacters() {
        assertEquals("José André Gonçalves", NameUtils.toTitleCase("JOSÉ ANDRÉ GONÇALVES"));
    }

    @Test
    void toTitleCase_shouldBeIdempotent() {
        String once = NameUtils.toTitleCase("  MARIA   DA silva  ");
        assertEquals(once, NameUtils.toTitleCase(once));
    }
}

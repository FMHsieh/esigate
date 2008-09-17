package net.webassembletool.test.cases;

import net.webassembletool.test.junit.HttpTestCase;

/**
 * Junit tests using the "/master" webapp
 * 
 * @author Omar BENHAMID
 */
public class MasterTest extends HttpTestCase {

    public void testBlock() throws Exception {
	doGet("/master/block.jsp");
	assertStatus(200);
	assertBodyContains("&lt;--image g�r�e par le provider</div>");
    }

    public void testBlockAndReplace() throws Exception {
	doGet("/master/replaceblock.jsp");
	assertStatus(200);
	// S'assurer qu'on a bien r�cup�r� quelque chose depuis le provider.
	assertBodyContains("&lt;--image g�r�e par le provider</div>");
	// S'assurer qu'on a bien pu int�grer notre param�ttre
	assertBodyContains("Bloc 2 (was : Bloc)");
	// S'assurer qu'on a bien retir� l'ancienne valeur du bloc
	assertBodyNotContains("aqua\">Bloc de contenu<br");
    }

    public void testMasterWebapp() throws Exception {
	assertBodyGetEqualsLocalFile("master");
    }

}

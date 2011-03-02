////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.css;

import org.apache.batik.css.parser.LexicalUnits;

import java.text.MessageFormat;

public class StyleParserErrorTranslator
{
    public static String getUserFriendlyErrror(String batikMessage) {

        String userFriendlyMessage = batikMessage;

        try {
            if (batikMessage.startsWith("Unexpected token:")) {
                MessageFormat batikMessageFormat = new MessageFormat("Unexpected token: {0,number,integer} (see LexicalUnits).");
                Object tokens[] = batikMessageFormat.parse(batikMessage);
                int errorCode = ((Long)tokens[0]).intValue();

                switch (errorCode) {
                    case LexicalUnits.ANY:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '*').";
                        break;
                    case LexicalUnits.AT_KEYWORD:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '@ident').";
                        break;
                    case LexicalUnits.CDC:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '-->').";
                        break;
                    case LexicalUnits.CDO:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: ''').";
                        break;
                    case LexicalUnits.EOF:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: 'EOF').";
                        break;
                    case LexicalUnits.LEFT_CURLY_BRACE:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '{').";
                        break;
                    case LexicalUnits.RIGHT_CURLY_BRACE:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '}').";
                        break;
                    case LexicalUnits.EQUAL:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '=').";
                        break;
                    case LexicalUnits.PLUS:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '+').";
                        break;
                    case LexicalUnits.MINUS:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '-').";
                        break;
                    case LexicalUnits.COMMA:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: ',').";
                        break;
                    case LexicalUnits.DOT:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '.').";
                        break;
                    case LexicalUnits.SEMI_COLON:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: ';').";
                        break;
                    case LexicalUnits.PRECEDE:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '>').";
                        break;
                    case LexicalUnits.DIVIDE:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '/').";
                        break;
                    case LexicalUnits.LEFT_BRACKET:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '[').  To set styles of type Array, use a comma delimited list.  For example, { foo: red, blue, green }.";
                        break;
                    case LexicalUnits.RIGHT_BRACKET:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: ']').";
                        break;
                    case LexicalUnits.LEFT_BRACE:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '(').";
                        break;
                    case LexicalUnits.RIGHT_BRACE:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: ')').";
                        break;
                    case LexicalUnits.COLON:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: ':').";
                        break;
                    case LexicalUnits.SPACE:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected space).";
                        break;
                    case LexicalUnits.COMMENT:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected comment).";
                        break;
                    case LexicalUnits.STRING:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected string).";
                        break;
                    case LexicalUnits.IDENTIFIER:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected identifier).";
                        break;
                    case LexicalUnits.IMPORTANT_SYMBOL:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '!important').";
                        break;
                    case LexicalUnits.INTEGER:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected integer).";
                        break;
                    case LexicalUnits.DASHMATCH:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '|=').";
                        break;
                    case LexicalUnits.INCLUDES:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '~=').";
                        break;
                    case LexicalUnits.HASH:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '#').";
                        break;
                    case LexicalUnits.IMPORT_SYMBOL:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '@import').";
                        break;
                    case LexicalUnits.CHARSET_SYMBOL:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '@charset').";
                        break;
                    case LexicalUnits.FONT_FACE_SYMBOL:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '@font-face').";
                        break;
                    case LexicalUnits.MEDIA_SYMBOL:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '@media').";
                        break;
                    case LexicalUnits.PAGE_SYMBOL:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected token: '@page').";
                        break;
                    case LexicalUnits.DIMENSION:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected dimension).";
                        break;
                    case LexicalUnits.EX:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected ex units).";
                        break;
                    case LexicalUnits.EM:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected em units).";
                        break;
                    case LexicalUnits.CM:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected cm units).";
                        break;
                    case LexicalUnits.MM:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected mm units).";
                        break;
                    case LexicalUnits.IN:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected in units).";
                        break;
                    case LexicalUnits.MS:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected ms units).";
                        break;
                    case LexicalUnits.HZ:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected ms units).";
                        break;
                    case LexicalUnits.PERCENTAGE:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected percentage units).";
                        break;
                    case LexicalUnits.S:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected S units).";
                        break;
                    case LexicalUnits.PC:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected pc units).";
                        break;
                    case LexicalUnits.PT:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected pt units).";
                        break;
                    case LexicalUnits.PX:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected px units).";
                        break;
                    case LexicalUnits.DEG:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected deg units).";
                        break;
                    case LexicalUnits.RAD:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected rad units).";
                        break;
                    case LexicalUnits.GRAD:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected grad units).";
                        break;
                    case LexicalUnits.KHZ:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected khz units).";
                        break;
                    case LexicalUnits.URI:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected URI).";
                        break;
                    case LexicalUnits.FUNCTION:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected function).";
                        break;
                    case LexicalUnits.UNICODE_RANGE:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected unicode range).";
                        break;
                    case LexicalUnits.REAL:
                        userFriendlyMessage = "Invalid CSS syntax(Unexpected real number).";
                        break;
                }
            }
            else if (batikMessage.equals("Invalid identifier start character: _.")) {
                userFriendlyMessage = "Invalid character found in CSS. "+
                        "Use quotes to surround font family names such as '_sans'.";
            }
            else if (batikMessage.equals("character")) {
                userFriendlyMessage = "Unable to parse CSS file.";
            }
        } catch (Exception e) {
            // just returns the original message
        }

        return userFriendlyMessage;
    }
}

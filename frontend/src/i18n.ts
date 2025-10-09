import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";

i18n
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
        detection: {
            order: ["navigator"],
            lookupFromPathIndex: 0,
        },

        fallbackLng: 'en',
        supportedLngs: ['en', 'nl'],
        load: 'languageOnly',

        resources: {
            en: {
                translation: {
                    index_title: "Yivi attributes from iDIN",
                    index_header: "Your Yivi attributes from your bank, via",
                    index_explanation:
                        "You can log in here via your bank, in the usual way. Your bank will then send, with your permission, some attributes (your name, address, etc.) back to this website. You can then load these attributes into your Yivi app.",
                    index_selectbank: "Select your bank to continue:",
                    index_defaultoption: "Choose your bank",
                    index_start: "Start iDIN verification",
                    error_title: "Error loading iDIN attributes",
                    error_header: "Error message",
                    error_text1: "Return to the {URL=index.html}iDIN issue page{/URL}, ",
                    error_text2: "or to the {URL=..}general issuance page{/URL}.",
                    error_generic: "An unexpected error has occurred.",
                    error_invalidbankcode: "The selected bank code is invalid.",
                    error_bankunavailable: "It is currently not possible to use iDIN for {{bank}}. Please try again later.",
                    error_invalidurl: "Received redirect URL is invalid.",
                    enroll_title: "Load basic attributes with iDIN",
                    enroll_header: "Available attributes",
                    enroll_received_attributes:
                        "The following attributes were provided by your bank:",
                    enroll_derived_attributes:
                        "The following attributes were derived from them:",
                    enroll_two_credentials_warning:
                        "In November 2019, a change was made to the structure of iDIN attributes in the Yivi app, where age thresholds are no longer provided separately. To support a smooth transition, these thresholds are temporarily issued twice: once as part of the full set of iDIN attributes and once separately. In the course of 2020, the separate age threshold attributes will be phased out.",
                    enroll_clicktoload:
                        "Click the Load Attributes button to load these into your Yivi app.",
                    enroll_load_button: "Load attributes into your Yivi app",
                    done_title: "iDIN Attributes Issued",
                    done_header: "iDIN attributes have been loaded",
                    done_text1:
                        "Congratulations! Your attributes have now been loaded into your Yivi app. They are now visible there.",
                    done_text2:
                        "You can now use them on any website that accepts these attributes.",
                    done_text3:
                        "Click the button below to test this; it will display your date-of-birth attribute.",
                    done_show_button: "Show date-of-birth attribute",
                    done_return: "Return to the attribute issuance page",

                    attribute_zipcode: "Postal code",
                    attribute_address: "Address",
                    attribute_city: "City",
                    attribute_initials: "Initials",
                    attribute_familyname: "Last name",
                    attribute_gender: "Gender",
                    attribute_dateofbirth: "Date of birth",
                    attribute_country: "Country",
                    attribute_telephone: "Phone number",
                    attribute_email: "Email address",
                    attribute_over12: "Over 12",
                    attribute_over16: "Over 16",
                    attribute_over18: "Over 18",
                    attribute_over21: "Over 21",
                    attribute_over65: "Over 65",

                    verify_success: "Success!",
                    verify_result: "Result",
                    verify_birthdate: "Your date of birth is:",
                    verify_cancelled: "Cancelled!",
                    verify_failed: "Failed!",
                },
            },
            nl: {
                translation: {
                    index_title: "Yivi attributen vanuit iDIN",
                    index_header: "Uw Yivi attributen vanuit uw bank, via",
                    index_explanation:
                        "U kunt hier inloggen via uw bank, op de gebruikelijke wijze. Uw bank stuurt dan, na uw toestemming, enkele attributen (uw naam, adres, etc.) terug naar deze website. Vervolgens kunt u deze attributen in uw Yivi app laden.",
                    index_selectbank: "Selecteer uw bank om door te gaan: ",
                    index_defaultoption: "Kies uw bank",
                    index_start: "Start iDIN verificatie",
                    error_title: "Fout bij het laden van iDIN attributen",
                    error_header: "Foutmelding",
                    error_text1:
                        "Keer terug naar de {URL=index.html}iDIN issue pagina{/URL}, ",
                    error_text2: "of naar de {URL=..}algemene uitgifte pagina{/URL}.",
                    error_generic: "Er is een onverwachte fout opgetreden.",
                    error_invalidbankcode: "De geselecteerde bankcode is ongeldig.",
                    error_bankunavailable: "Het is op dit moment niet mogelijk om iDIN te gebruiken voor {{bank}}. Probeer het later nog een keer.",
                    error_invalidurl: "Ontvangen redirect-URL is ongeldig.",
                    enroll_title: "Basis attributen laden met iDIN",
                    enroll_header: "Beschikbare attributen",
                    enroll_received_attributes:
                        "De volgende attributen zijn beschikbaar gesteld door uw bank:",
                    enroll_derived_attributes:
                        "De volgende attributen zijn daaruit afgeleid:",
                    enroll_two_credentials_warning:
                        "In november 2019 is een verandering in de structuur van de iDIN attributen in de Yivi-app doorgevoerd, waarbij leeftijdsgrenzen niet meer apart staan. Om deze verandering soepel te laten verlopen, worden deze leeftijdsgrenzen tijdelijk dubbel uitgegeven: eenmaal als onderdeel van het geheel van iDIN attributen en eenmaal los. In de loop van 2020 zal de uitgifte van de losse leeftijdsgrensattributen definitief worden uitgefaseerd.",
                    enroll_clicktoload:
                        "Klik de Laad attributen knop om deze attributen in uw Yivi app te laden.",
                    enroll_load_button: "Laad attributen in uw Yivi app",
                    done_title: "iDIN Attributen uitgegeven",
                    done_header: "iDIN attributen zijn geladen",
                    done_text1:
                        "Gefeliciteerd! Uw attributen zijn nu in uw Yivi app geladen. Daar zijn ze nu zichtbaar.",
                    done_text2:
                        "U kunt ze nu gebruiken op iedere website die deze attributen accepteert.",
                    done_text3:
                        "Door op de knop hieronder te klikken kunt u dit testen; u toont dan uw geboortedatum-attribuut.",
                    done_show_button: "Toon geboortedatum-attribuut",
                    done_return: "Keer terug naar de attribuut-uitgifte pagina",

                    attribute_zipcode: "Postcode",
                    attribute_address: "Adres",
                    attribute_city: "Stad",
                    attribute_initials: "Initialen",
                    attribute_familyname: "Achternaam",
                    attribute_gender: "Geslacht",
                    attribute_dateofbirth: "Geboortedatum",
                    attribute_country: "Land",
                    attribute_telephone: "Telefoonnummer",
                    attribute_email: "E-mailadres",
                    attribute_over12: "Over 12",
                    attribute_over16: "Over 16",
                    attribute_over18: "Over 18",
                    attribute_over21: "Over 21",
                    attribute_over65: "Over 65",

                    verify_success: "Succes!",
                    verify_result: "Resultaat",
                    verify_birthdate: "Uw geboortedatum is:",
                    verify_cancelled: "Geannuleerd!",
                    verify_failed: "Mislukt!",
                },
            },
        },

        interpolation: {
            escapeValue: false, // react already escapes
        },
    });

i18n.on('languageChanged', (lng) => {
    if (typeof document !== 'undefined') {
        document.documentElement.lang = lng;
    }
});


export default i18n;

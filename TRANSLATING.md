# Translations
We hope to allow anyone to use the website and application, this means translating literally everything. Contributing translations is very different from contributing code changes, which is why I've made a document specifically for translating.

Before you begin, you need to clone the **development** branch. You only need to worry about the `locales` folder for translating! It contains XML files with all the strings found in the website and app. You need to translate from English to your language of choice, you must not translate from another language. See the rest of the development branch to find out where those strings are specifically used, as it will help you properly translate.

## Guidelines
Please read the rules below to make sure you are translating correctly.

- Regardless of what language you want to translate, you must do it from the original English version. Translating from other translations will certainly result in information being lost and/or a poor translation.
- You must be fluent in English. We should know from your pull request description. If we can't understand you, your translations will not be accepted. We might give you a simple test just to check.
- You have to do translations in the file for the language you are translating to. If you are translating into Spanish, you must do translations in the `locales/lang-es.xml` file!
- If you want to translate a language that doesn't have its own file yet, submit an issue and request that we create the file for you. **DO NOT** try to create a new language file yourself!
- You must put your name in a comment next to each translation. See below for an example translation.

```xml
<!-- In lang-en -->
<string name="greeting">Hello!</string>

<!-- Translation in lang-es -->
<string name="greeting">¡Hola!</string> <!-- By: [your name] -->
```
You can also confirm translations by adding your name to the comments. See below for an example with 3 confirmations.

```xml
<string name="greeting">¡Hola!</string> <!-- By: Ben. Confirmed: Emily,
                                        Mateo, Andrés -->
```

No more than 5 confirmations are needed for any translation. If a translation has less than 5 or no confirmations, consider confirming it or changing it. If you are changing a translation, please add the previous translation to a comment below it. We won't accept changes to existing translations if they don't include the previous one.

```xml
<string name="action">comienzo</string> <!-- By: [your name] -->
<!-- <string name="action">empezar</string> <!-- By: Ben -->
```

## Disclaimer
Because everyone makes mistakes, and information can literally be lost in translation, you'll have to bear with us as we try to figure out what translations are good and which aren't. A translation with 5 confirmations could easily be incorrect due to a lack of context.

No translations are final until we manually add them to the master branch. If you submitted a translation which is accepted, no matter how small, we will include the name in the comment in a credits section for the app or website. We won't add your name if you only confirmed a few translations.

If you wish to have your name excluded from the credits, use the name "anon" instead and we won't include you.

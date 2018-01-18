### Note to Contributors
This project is not even at version 1.0 yet! This means that so much is likely to change that your time might be better spent elsewhere while I get this project to a stable state. If you like where it's going so far, star it! If you are dying to contribute, please just open an issue or wait until version 0.8 is released.

<hr/>

# Contributing Guide
Thank you for taking interest in contributing to this project! There are many ways that you can contribute: issue fixing, translations, feature additions, and pretty much anything else.

#### General Contributions
If you are just looking to help out, consider finding issues tagged **TODO**. These are ones we have accepted and plan to fix/ implement. The easiest way to get your commits accepted is to work on **TODO** issues. You can also scan the code and optimize it or fix problems I haven't found yet.

#### Feature Requests
If you have a feature idea, you should open an issue talking about it first to make sure it lines up with the goals for CSGO Skill. I'd hate for you to spend time creating a feature only to have it denied because it didn't fit.

#### Translations
If you want to translate portions of the website or application, see [the TRANSLATING file](TRANSLATING.md)

#### Other Contributions
If you want to contribute in a way other than listed here, open a `[QUESTION]` issue or email the <a href="mailto:devteam@csgo-skill.com">devteam@csgo-skill.com</a>

## Dependencies
You should install **[Git](https://git-scm.com/downloads)** on your computer. It let's you run console commands to interact with GitHub repositories. I recommend you also install **[GitHub Desktop](https://desktop.github.com/)**, it makes it a lot easier to make commits and change branches without even opening the console.

The only software you *need* is the latest version of Android Studio. It lints your code and is how we build the apk. Hopefully your Android device came with a USB connector, which you can simply plug into your computer and directly install the local code to your device. For more information on using Android Studio, just browse the **[Android Developer website](https://developer.android.com/index.html)**.

Personally, I use the **[Atom text editor](https://atom.io/)** for everything else. It has tons of features, and just about any other features can be downloaded with the built-in package installer. Because I'm a code wizard, I don't actually run a local server to test my website code. But, you probably should use **[XAMPP](https://www.apachefriends.org/index.html)**, it's the closest to our current server setup. There isn't much setup, just point the root document to the `public_html` directory and the site should start up.

There is one obvious problem, however, you won't be able to access our databases. This will never be something we allow anyone to do. You should consider looking how to setup a local MySQL database if your code changes need a database.

## Databases
The most optimal way to contribute is by submitted bug fixes, but some of you may need to work directly with our databases. The best way to do this is to visit the **Wiki** tab, it's at the top of any page on the repository. There you can see our exact database structure, so you can recreate it on your local machine and do tests there. You'll have to create two files called `dbInf.php` and `setup.php` which simply define some keywords and helper functions used frequently on the website.

## Comments
You should always comment your code. Contributing a lot of code with no comments will make it harder to accept your work. All the same, contributing more comments than code is distracting and makes actual changes harder to spot. It will also make the code base harder for others to understand and work with. Be a good programmer and comment your code where necessary, or where you feel certain operations might be confusing and could benefit from a short explanation. *Do not include your name in any comments unless otherwise specified, like with translations.*

## Testing
At the current moment, there is no testing for the website or app other than just physically messing around with the app. We try to develop the website and app to be very modular and with excellent error reporting so that problems are easily traceable, isolated, and fixable with minor adjustments.

To get an idea for how to report errors, just look at the code. I trust that you know how to properly test changes by setting up your own tests or by just using the app or website itself. More extensive testing is done before accepting code changes, that way you can just focus on coding.

## Contributing/ Submitting changes
Control of *this* project is ultimately mine, but I still want to allow the project to grow publicly. If you don't like where the project is going, speak up and contribute before cloning it.

Follow these instructions when contributing code/ content changes.
- Clone the repository
  - Example:
    ```
    git clone https://github.com/almic/csgo-skill
    ```
    You can also simply use GitHub Desktop to clone it by going to File **>** Clone Repository **>** URL, and typing `almic/csgo-skill` in the box
- Create a new branch based on **development** and rename it to what you intend to do
  - Example:
    ```
    git checkout -b BRANCH_NAME origin/development
    ```
  - Create different branches for separate features or fixes
- Make your changes
  - Bug fixes, feature implementations, commenting, etc
  - Test the application by building it directly to your device **OR**
    test the website by running a local server with XAMPP
- Commit your changes
  - Use GitHub desktop, it forces you to create a title and message so that commits look nicer
  - Line one of the commit is what's shown in the change log. The rest are body lines that should describe specific changes.
  - Although I don't do it often because the project is still being heavily developed, you should be a [Commitizen](https://github.com/commitizen/cz-cli) since other people will have to review your commits
- Make the pull request
  - You must request against the **development branch**
  - Describe the pull request so we know what to expect
  - If you are denied, we will tell you why and most of the time will tell you how to change it so that we can accept it

Following these instructions will allow your pull request to be accepted faster!

## :anger: Legal Disclaimer
This project is open-source and the code is licensed under the [MIT license](http://opensource.org/licenses/mit-license.php), and the content is licensed under the [Creative Commons Attribution 3.0 license](http://creativecommons.org/licenses/by/3.0/us/deed.en_US), meaning that literally anyone can look under the hood and contribute how they want. Although we shouldn't have to tell you, we won't accept just any contribution. When you interact with **this** project in any way, you are subject to the [User Agreement](TERMS.md). See our [Privacy Policy](PRIVACY.md) to make sure you adhere to it when making changes. The Privacy Policy and the User Agreement do not supplement the MIT license.

## Credit
By contributing to this project, you will also be added to the credits section of the app or website unless you specifically request to be excluded. **Do not add your name yourself!** If you want to use a name besides the name on your GitHub profile, mention it in the PR.

:heart: Thanks for contributing!

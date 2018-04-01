## Introduction
The Current Project page is where all current planned features for the next update are located. Outbound links are there to reduce the size of this page and allow for more open discussion and reasoning. If a feature catches your eye, please follow the link to read more about it. This page is not exhaustive, and some planned features are so new they aren't written about yet.

For me, the developer, this page is where I go to put down some rough thoughts and list out my next work tasks. Hopefully this page stays up-to-date, but if I get a new idea I'll probably spend a few days working on it before formally writing it down. If you want to share some new ideas, open an issue and put "IDEA" in the title followed by a short description of the idea. I can pretty much guarantee that I'll read it and respond in a few hours.

## Project: Initial Release
**Version**: v0.1.0

**Focus**: Initial release of the android app

**Goals**:
- Automatic CSGO API stat tracking
- Android app to show these stats
- Instant Steam logins
- Optional signup to unlock advanced features
- No cost, no fees, no micro transactions, and ad-free
- Fast access to favorite stats, no internet required (caching)

**Description**:  
The initial release is heavily limited to simply pushing out a polished and good android app, backed by functional web servers tracking daily stats and storing them for later viewing. Future endeavors will add a website and an iOS port, as well as new features available to users.

## Feature List

- App
  - Steam login
  - Signup
  - Stats at a glance
  - Charts and Graphs
  - Email changing
  - Persona name changing
  - Choice of backgrounds
  - Background stat syncing

### Steam Login
> [Issue #33](../issues/33)

Anyone who meets the minimum requirements listed below can login and get a `player` account, which has limited tracked as set [here](Database-&-Tracking#limited-tracking).

Requirements:
- Public profile  
- 10 hours in CS: GO

### Signup
> [Issue #32](../issues/32)

`Players` can sign up for a CSGO Skill account at any time. There is a special place in the App to do this, and they will need to add an email address and select a custom username. They also have the option to select a new custom persona, by default it is their current Steam persona.

Currently this only unlocks full stat tracking features, but more is planned for the future.

### Stats at a Glance (Home fragment)
> [Issue #31](../issues/31)

The main screen, or Home fragment in source, shows all major stats in a scrollable view. It selectively choses what to show based on the available stats. By default it collates the previous two weeks into sums and averages.

Likely Order:
- Time played
- Kills/ Deaths ratio
- Accuracy %, Headshot %
- Damage
- Round Win %, Match Win %
- Bomb Plants, Defuses, and Hostage rescues
- MVPs, Contribution Score
- Money Earned
- Knife, Grenade, Fire and Flash Kills
- Round Win % on Dust 2, Inferno, Nuke, Train, Cobblestone
- Kills and Accuracy % with rifles, pistols, SMGs, and heavy weapons

### Stat History
> [Issue #34](../issues/34)

Most percentage data can be shown as a pie circle, with the ratio or percentage in the center, and number on the left and right. Or they can simply be shown as the full number.

When showing stat history, a simple line graph is good enough to show changing totals, ratios and percentages. For things like win rate, we may instead show a stacked bar chart with loses on the bottom in red and wins on the top in green. To start with, only a line graph is good enough, but adding more visualization options is definitely planned for a future update.

### Account Info Changing

In the app, for `users`, is the option to change your connected email address, username, and persona.

- **Username**: The user simply types in a new username, and is sent to the server. If the username is not in use, it changes and the server tells the client that the operation succeeded.
- **Persona**: Identical to the above, except that no uniqueness requirement exists. Whatever the user wants is what the user gets. However, they must tell the server to use a custom persona and not to update it based on Steam player summaries. This is represented by simply checking a "use custom persona" box in the app.
- **Email**: This requires that the user verify the new email address by clicking a provided link sent to them. Until the user clicks the link, the original email is used for whatever they asked us to use it for. The user is given an hour to do this, after which the link dies and they'll have to repeat the process. An email is also sent to the old address notifying the change, which provides a link to some moderation place that they can use to reverse the change if it was not intended. This process of moderation is still in the works, and will likely come after initial release.

### App Background Selection

It'll be nice to allow players to select different app backgrounds, so we'll have that for initial release. For now it'll probably just be a list of maps they can pick, and the app will immediately change the image so they can see it.

### Stat Syncing

Perhaps twice per day the app will, NOT by default, only if expressly allowed by choosing so in the settings, download the current stats for users on the device. This won't run a login, simply poll api.csgo-skill.com/stats/ and update all the stats. This would effectively allow users to keep their account on the list for stat updates without having to login, and allow everyone to see recent stats if they once had internet and no longer do. For instance, they get on a plane and forgot to open the app, but luckily it synced data a few hours earlier so the information is up-to-date.

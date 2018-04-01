**v0.1-alpha**
# Introduction
Welcome to the hub for all that is database storage and tracking. Everything here is OFFICIAL, meaning it is the way the project currently works in its most recent version. Other versions will be stored in separate pages as they become outdated. Currently there isn't even a release yet, so everything here is kinda unstable. However, it will probably not change much since this page is already very refined, and most systems already use everything as described here.

# Table of Contents
> [**Introduction**](#introduction)

> [**Table of Contents**](#table-of-contents)

> [**DB Structure**](#db-structure) - Basic information about databases

> [**Basic Tracking**](#tracking) - Basic information about tracking

> [**Website**](#website) - Information about the website structure
>> [**Private Information**](#private-information) - Specific info that is private for all accounts  
>>
>> [**Players**](#collection-players) - The `Players` collection structure  
>> [**Stats**](#collection-stats) - The `Stats` collection structure  
>> [**Token Generator**](#token-generator) - The method for creating login tokens for accounts  
>> [**Token Use**](#token-use) - More information about tokens

> [**App**](#app) - Information about the app structure
>> [**Users**](#table-users) - The `Users` table structure

> [**Validation**](#validation) - How information like usernames and emails are validated
>> [**Emails**](#email-validation) - The requirements for email addresses  
>> [**Usernames**](#username-validation) - Formation of usernames from personas

> [**Tracking**](#tracking-1) - Detailed tracking information
>> [**Stat Conversion Table**](#stats-conversion) - Conversion table for stats (Steam API => Skill API)  
>> [**Limited Tracking**](#limited-tracking) - How stat tracking works on `player` accounts  
>> [**User Tracking**](#user-tracking) - How stat tracking works on `user` accounts  
>>
>> [**Current Stats**](#current-stats) - Details of `current` stats in DB  
>> [**Grand Stats**](#grand-stats) - Details of `grand` stats in DB  
>> [**History Stats**](#history-stats) - Details of `history` stats in DB  
>> [**Mega Stats**](#mega-stats) - Details for site-wide stats in DB  
>>
>> [**Example Current**](#example-current) - Example document for current stats  
>> [**Example Grand**](#example-grand) - Example document for grand stats  
>> [**Example History**](#example-history) - Example document for history stats


# DB Structure
Website: MongoDB  
App: Built-in SQLite Database (android)  
Communication: Strictly JSON

# Tracking
The website itself is where ALL the tracking happens. While we could save time and resources by having the app do all the heavy-lifting like making the API request, parsing it, saving the important bits, and then sending it to us, that would simply make spoofing stats ridiculously easy. Originally that is how I designed it, long before this repository existed, but I realized it would be far more consistent and "secure" to do all that on the server.

There is only one time that we really care about, the start of each day. At exactly 00:00-UTC, we update all users stats and save for the previous day. For example, a user living in Moscow will see their Feb. 14th stats at the earliest 03:00-Feb. 15th local time. A user in New York will see their Feb. 14th stats at the earliest 19:00-Feb. 14th local time.

In future versions, the user will be asked to select a timezone that best fits them, so that they can see their daily stats on the proper dates.

# Website
The website runs on Node.js and uses MongoDB for storage. The site is hosted by Heroku. The database is hosted by the official MongoDB website, although the service is through Amazon or something like that. The actual website is a single script that serves up html documents and handles API things, so it is constantly running and listening for requests. It runs other scripts which handle tasks, mainly just checking for CS: GO updates and stat tracking.

In short, `server.js` listens for requests, and other processes handle time sensitive tasks in a queue-worker structure (thanks, [Nque](https://github.com/almic/nque)!)

This project uses MongoDB for all it's data storage.

## Private Information
I would like to take a moment to discuss why I think the email address and link to all user profiles and Steam IDs should be kept private. It's obvious why we should not reveal a player's email, but is not obvious why we should hide the profile link and Steam ID.

#### Profile Links
In order to reduce spam directed towards players, we will not display anyone's profile link. This is because in the future I plan on adding player leaderboards, which would directly link to their CSGO Skill profile pages. Since we would not offer an easy way to find the player's Steam account, it greatly reduces spam and keeps them safer-ish.

#### Steam IDs
If you reveal a profile link, you reveal the Steam ID. As the system internally uses the Steam ID for API requests of non-user accounts, this will not apply to them. But for user accounts, their selected username will be used for APIs instead, allowing more anonymity. This is to prevent an easier form of impersonation; simply linking to a profile with good stats that has the same picture and persona, impersonators can easily play off that those stats are theirs.

However, by not displaying a direct link back to the profile on our website, everyone must take that claim with a large grain of salt. In the future custom bios will be added, which forces users to reveal their actual Steam info, specifically the Steam ID or link, and also let's them make others aware of stat thieves, or add special codes and words to the bio for auditing. This will thwart impersonators by removing the ease of doing this, and can help other users determine an impersonator by simply asking them to add code words to their bio to prove ownership of the account and therefore legitimacy of the stats.

Anyone who refuses to do that is clearly impersonating and not worth the time.

## Collection: `Players`
This collection stores basic profile information about all players who have an account. It does not include players who simply add themselves to basic stat tracking.

```js
/* The currently connected Steam account ID, unique */
steam_id: "0123456789"

/* The login token for the account, expires 6 days after creation, and is
   reset every time the player logs into their device */
token: {time: 0, value: "abcdef0123456789"}

/* Custom persona name, whatever the user wants */
persona: "Almic"

/* The direct link to the player's Steam profile, shortened for space */
profileurl: "profiles/76560000000000000"

/* The direct link to the player's Steam avatar, shortened for space */
avatarurl: "aa/aabbccddeeff00112233445566778899"

/* Bragging rights, milliseconds since Epoch */
created: 1520714735232

/* An object for holding various account status information. */
status: {level: "admin"}

/* Custom username, lowercased, URL friendly, unique */
username: "almic"

/* Unique email address, mostly for spam reduction */
email: "devteam@csgo-skill.com"

/* Email verification status of an account */
verified: true

/* Alpha feature, currently exists only for myself */
subscription: { csgoupdates: true }

```

Here's a table for some more specific details about each key.

| Key | Value | Properties |
| :---: | :---: | :---: |
| `steam_id` | String | Unique, 16-18 chars, 0-9 only |
| `token` | Object | — |
| `persona` | String | 3-35 length |
| `profileurl` | String | Shortened for storage |
| `avatarurl` | String | Shortened for storage |
| `created` | Integer | Milliseconds since Epoch |
| `status` | Object | Special back-end info |
| `username` | String | Unique, url-encoded[*](#username-validation), Max 35 length |
| `email` | String | Unique, Valid[*](#emails), Max 50 length |
| `verified` | Boolean | — |
| `subscription` | Object | Options for special email lists |


## Collection: `Stats`
This collection is where all the bulk of the information is. This is where all the stats history is saved for everyone, and some other extra stats.

```js
/* Steam ID for the user stats that are stored here. NEVER changes once set! */
steam_id: "0123456789"

/* Helps the site determine how to track stats for this Steam ID */
user: true

/* Holds some basic info about the last 20 visits a profile received */
visits: [
    {
        user: "anon",
        time: 1520700000000
    },
    {
        user: "012345689",
        time: 1520600000000
    }
]

/* Used to determine if a user account is "inactive" */
active: 1520714738048

/* The active API response, updated at least daily or more */
current: {...}

/* Our own record of grand-total achievement for an account */
grand: {...}

/* A long record of statistics history, updated daily */
history: {...}

// For more info on 'current', 'grand', 'history' et al., keep scrolling
```

Here's a table for some more specific details about each key.

| Key | Value | Properties |
| :---: | :---: | :---: |
| `steam_id` | String | Unique, 16-18 chars, 0-9 only |
| `user` | Boolean | — |
| `visits` | Array | Max 20 length, format as above |
| `active` | String | Date-Time like `YYYY-MM-DD'T'HH:MM:SS'Z'` |
| `current` | Object | [Tracking: `current`](#current) |
| `grand` | Object | [Tracking: `grand`](#grand) |
| `history` | Object | [Tracking: `history`](#history) |

## Token Generator
To make sure we are talking to the actual owner of an account, we use a randomly generated SHA256 token, and give it a timestamp on the server so we know when to expire it. The method is pretty straight-forward

```js
var token = {
    'time': +new Date(),
    'value': crypto.createHash('sha256').update(crypto.randomBytes(128)).digest('hex')
}
```

## Token Use
Really only used to perform instant-logins and change account info. Currently nothing very sensitive is saved to accounts, only an email address. If a hacker really wanted email addresses, they might have a better chance somewhere else than trying to brute force secrets for one email at a time.

This is also the exact same thing used for the website, when I eventually implement one.

# App
The app is currently only available for Android, but I definitely want to make it for iOS in the future. The app uses the built-in library `SQLiteDatabase`, or SQLite for short, to save basic account data. For the moment, the app needs no active internet connection to show stats, since all are cached when possible.

At the start, all players login and automatically set up a `player` account with limited history and grand stat tracking. They can sign up for a CSGO Skill account by connecting an email and choosing a username, and this will unlock full stat tracking for everything.

See the [Tracking Section](#tracking-1) for more details on limited tracking.

## Table: `Users`
This is currently the only table, and it stores all account information to identify the user online and offline. It also stores all stat information so if they lose internet, they can still access whatever was last cached.

```js
// Yeah this is how I'm going to represent an SQL table.

/* Steam ID for the user, unique and only for queries */
steamID: "0123456789"

/* Identical to the server 'Players' collection format, stringified */
profile: "{ ... }"

/* Identical to the server 'Stats' collection format, stringified */
stats: "{ ... }"
```

Here's a table for some more specific details about each column.

| Column | Type | Properties |
| :---: | :---: | :---: |
| `steamID` | TEXT | Unique |
| `profile` | TEXT | `JSON.stringify()` |
| `stats` | TEXT | `JSON.stringify()` |

No validation needs to be done for any data, since it should match what the server has at that time, which was previously validated.

# Validation
User info is validated using some pretty specific rules. Even the Steam ID needs to meet some rules, despite coming directly from Steam. Some of these are redundant, but I think closing a hole is better than assuming it doesn't exist.

Here's a table of the things the user can provide us, and some basic rules. If you want more information, some of them have their own detailed sections on why the rules were chosen.

| Name | Rules |
| :---: | :---: |
| `steam_id` | 16 - 18 chars, all numbers, unique. |
| `token` | `value:` 64 chars, `time:` within 6 days |
| `persona` | 3-35 chars. HTML encoded afterwards |
| `email` | 4-50 chars, [other requirements](#email-validation), unique. |
| `username` | 3-35 chars, [other requirements](#username-validation), unique. |

These are the primary things we ask the user or the device for frequently, and are the most important parts of account creation. Before we ever shove them into a collection or search for them, we must match them all against these rules. It reduces security holes and keeps the database resources free from queries that would obviously fail.

## Email Validation
Emails are the mostly for spam reduction, only to prevent the creation of spam accounts or the use of multiple accounts to one email. Future plans for newsletters and other emails exist, but only if expressly requested by users, and absolutely nothing else.

From the great words of this [StackOverflow comment](https://stackoverflow.com/questions/46155/how-can-an-email-address-be-validated-in-javascript#comment32449168_46181):
> ***You*** cannot validate email addresses, period!! The only one who can validate an email address is the ***provider*** of the email address. So why would you try? For example, this answer says these email addresses: `%2@gmail.com, "%2"@gmail.com, "a..b"@gmail.com, "a_b"@gmail.com, _@gmail.com, 1@gmail.com , 1_example@something.gmail.com` are all valid, but Gmail will ***never*** allow any of these email addresses. You should do this by accepting the email address and sending an email message to that email address, with a code/link the user must visit to confirm validity.

Simply put, we shouldn't try to validate emails ourselves. Whatever the user gives us, we'll try and email it the link. If they gave us a valid email, then they'll be able to see our message and verify the account.

However there is but 2 requirements that I believe all emails should meet.
- 4-50 characters in length
- At least one `@` character, and is not the first character

Anything else is, to my knowledge, ridiculous and unlikely an email that a real person would use.

## Username Validation
When it comes to the persona, the only limit is 3-35 characters. However the username is used for URL linking, so it clearly needs to be strict. Currently the best way to go is to copy+paste the persona name when a user signs up, convert it to lowercase, and remove these specific characters.

| Removed |
| :---: |
| `.` `~` <code>&nbsp;</code> `:` `/` `\`<br/>`?` `&` `!` `#` `[]`<br/>`@` `$` `'` `"` `()`<br/>`*` `+` `,` `;` `=` `%`<br/>`^` <code>&#96;</code> `{}` <code>&#124;</code> `<>` |

The idea is to allow any sort of language characters like 'こんにちは' or 'Прощай', which can be used in URLs. By converting it to lower case, we can prevent people from impersonating others, like with Hello (`Hello`) and HeIlo (`HeIlo`). It also removes the worry, if someone was typing the link, to type the exact capitalization. Usernames must be validated by formatting it, then checking the length, then checking for uniqueness.

Since browsers process links as-is, and only the characters listed above could actually break links (technically only `/`, `&`, and `+` could break them), there is no reason to worry about imitating symbols like <code>&#921; (&amp;#921;)</code> which in some fonts can look like `|`, or <code>&#236; (&amp;#236;)</code> which can look like `i`, as these characters are hard to accidentally type and (in my opinion) easier to tell apart than I and l (`I` and `l`).

Either way, we should allow users to make impersonation reports (who would do that on our site anyway), in such cases that this becomes a problem.

# Tracking
The biggest aspect of the project is in depth tracking for every statistic possible for every individual player. Lucky for us, recording numbers doesn't really take up much space, especially with the method we have for doing so. This section will probably take up way more of the page than it should, but that's only because the examples here are complete.

### Visits
Earlier you may have noticed the `visits` key in the [Stats Collection](#collection-stats) section. This offers a way for us to skip tracking for accounts which are not active. In general, if we don't see any changes in stats for an account, we completely skip recording it in the collection. However we still consume precious time requesting the CSGO stats and comparing for changes.

By tracking how often the stats for an account are actually looked at, we can skip the check completely since no one seems to care anyway. The choices are somewhat arbitrary, but I think they make sense.

- API stat requests log an anonymous visit and record the time.
- If a user requests a profile's stats, it also records the user's Steam ID.
- If the owner logs in, update the `active` key with the current time.

When checking if the account should have stats tracked for the day, the server makes three checks. The first number is the minimum for limited accounts, and the second in [brackets] is for user accounts.

1. Is the `active` time is within the last 30 days? If so, track. Otherwise continue.
1. Is the account a `user`? If so, continue. Otherwise do not!
1. Are there any visits dated in the last week? If so, track. Otherwise do not!

This is simply to relieve the load on our servers by abandoning inactive accounts. For user accounts, we let visits force stat tracking.

## Stat Conversion
You may find the table below very helpful, as it offers shorter key names and their conversion to the stat in the CSGO API.

> | Name | Description | New Name |
> | :--- |    :---:    | :---     |
> | `total_kills` | — | `kills` |
> | `total_deaths` | — | `deaths` |
> | `total_time_played` | In round seconds | `time` |
> | `total_wins` | **R**ound **wins** | `rwins` |
> | `total_rounds_played` | — | `rounds` |
> | `total_matches_won` | **M**atch **wins** | `mwins` |
> | `total_matches_played` | — | `matches` |
> | `total_shots_fired` | — | `shots` |
> | `total_shots_hit` | — | `hits` |
> | `total_damage_done` | — | `damage` |
> | `total_planted_bombs` | — | `plants` |
> | `total_defused_bombs` | — | `defuse` |
> | `total_rescued_hostages` | — | `hostage` |
> | `total_contribution_score` | — | `contrib` |
> | `total_money_earned` | — | `money` |
> | `total_mvps` | — | `mvps` |
> | `total_kills_knife` | — | `knife` |
> | `total_kills_hegrenade` | — | `nades` |
> | `total_kills_molotov` | — | `fires` |
> | `total_kills_enemy_blinded` | — | `flash` |
> | `total_kills_against_zoomed_sniper` | — | `csnipe` |
> | `total_kills_enemy_weapon` | — | `backfire` |
> | `total_dominations` | — | `doms` |
> | `total_revenges` | — | `revs` |
> | `total_domination_overkills` | — | `overkill` |
> | `total_wins_pistolround` | — | `pistolwin` |
> | `total_weapons_donated` | — | `donation` |
> | `total_wins_map_de_cbble` | — | `wins_cbble` |
> | `total_wins_map_de_dust2` | — | `wins_dust2` |
> | `total_wins_map_de_inferno` | — | `wins_inferno` |
> | `total_wins_map_de_nuke` | — | `wins_nuke` |
> | `total_wins_map_de_train` | — | `wins_train` |
> | `total_rounds_map_de_cbble` | — | `rnds_cbble` |
> | `total_rounds_map_de_dust2` | — | `rnds_dust2` |
> | `total_rounds_map_de_inferno` | — | `rnds_inferno` |
> | `total_rounds_map_de_nuke` | — | `rnds_nuke` |
> | `total_rounds_map_de_train` | — | `rnds_train` |
> | `total_kills_headshot` | — | `heads` |
> | `total_shots_deagle` | Includes R8 | `g8s` |
> | `total_hits_deagle` | — | `g8h` |
> | `total_kills_deagle` | — | `g8k` |
> | `total_shots_elite` | Dual Berettas | `g3s` |
> | `total_hits_elite` | — | `g3h` |
> | `total_kills_elite` | — | `g3k` |
> | `total_shots_fiveseven` | Includes CZ75 | `g6s` |
> | `total_hits_fiveseven` | — | `g6h` |
> | `total_kills_fiveseven` | — | `g6k` |
> | `total_shots_glock` | — | `g0s` |
> | `total_hits_glock` | — | `g0h` |
> | `total_kills_glock` | — | `g0k` |
> | `total_shots_ak47` | — | `g12s` |
> | `total_hits_ak47` | — | `g12h` |
> | `total_kills_ak47` | — | `g12k` |
> | `total_shots_aug` | — | `g17s` |
> | `total_hits_aug` | — | `g17h` |
> | `total_kills_aug` | — | `g17k` |
> | `total_shots_awp` | — | `g18s` |
> | `total_hits_awp` | — | `g18h` |
> | `total_kills_awp` | — | `g18k` |
> | `total_shots_famas` | — | `g11s` |
> | `total_hits_famas` | — | `g11h` |
> | `total_kills_famas` | — | `g11k` |
> | `total_shots_g3sg1` | — | `g19s` |
> | `total_hits_g3sg1` | — | `g19h` |
> | `total_kills_g3sg1` | — | `g19k` |
> | `total_shots_galilar` | — | `g10s` |
> | `total_hits_galilar` | — | `g10h` |
> | `total_kills_galilar` | — | `g10k` |
> | `total_shots_m249` | — | `g31s` |
> | `total_hits_m249` | — | `g31h` |
> | `total_kills_m249` | — | `g31k` |
> | `total_shots_m4a1` | M4A4 & M4A1-S | `g14s` |
> | `total_hits_m4a1` | — | `g14h` |
> | `total_kills_m4a1` | — | `g14k` |
> | `total_shots_mac10` | — | `g21s` |
> | `total_hits_mac10` | — | `g21h` |
> | `total_kills_mac10` | — | `g21k` |
> | `total_shots_p90` | — | `g25s` |
> | `total_hits_p90` | — | `g25h` |
> | `total_kills_p90` | — | `g25k` |
> | `total_shots_ump45` | — | `g24s` |
> | `total_hits_ump45` | — | `g24h` |
> | `total_kills_ump45` | — | `g24k` |
> | `total_shots_xm1014` | — | `g28s` |
> | `total_hits_xm1014` | — | `g28h` |
> | `total_kills_xm1014` | — | `g28k` |
> | `total_shots_bizon` | — | `g26s` |
> | `total_hits_bizon` | — | `g26h` |
> | `total_kills_bizon` | — | `g26k` |
> | `total_shots_mag7` | — | `g30s` |
> | `total_hits_mag7` | — | `g30h` |
> | `total_kills_mag7` | — | `g30k` |
> | `total_shots_negev` | Laser Beam | `g32s` |
> | `total_hits_negev` | — | `g32h` |
> | `total_kills_negev` | — | `g32k` |
> | `total_shots_sawedoff` | — | `g29s` |
> | `total_hits_sawedoff` | — | `g29h` |
> | `total_kills_sawedoff` | — | `g29k` |
> | `total_shots_tec9` | Includes CZ75 | `g5s` |
> | `total_hits_tec9` | — | `g5h` |
> | `total_kills_tec9` | — | `g5k` |
> | `total_shots_hkp2000` | USP-S & P2000 | `g2s` |
> | `total_hits_hkp2000` | — | `g2h` |
> | `total_kills_hkp2000` | — | `g2k` |
> | `total_shots_mp7` | — | `g23s` |
> | `total_hits_mp7` | — | `g23h` |
> | `total_kills_mp7` | — | `g23k` |
> | `total_shots_mp9` | — | `g22s` |
> | `total_hits_mp9` | — | `g22h` |
> | `total_kills_mp9` | — | `g22k` |
> | `total_shots_nova` | — | `g27s` |
> | `total_hits_nova` | — | `g27h` |
> | `total_kills_nova` | — | `g27k` |
> | `total_shots_p250` | — | `g4s` |
> | `total_hits_p250` | — | `g4h` |
> | `total_kills_p250` | — | `g4k` |
> | `total_shots_scar20` | — | `g20s` |
> | `total_hits_scar20` | — | `g20h` |
> | `total_kills_scar20` | — | `g20k` |
> | `total_shots_sg556` | SG 55**3** | `g16s` |
> | `total_hits_sg556` | — | `g16h` |
> | `total_kills_sg556` | — | `g16k` |
> | `total_shots_ssg08` | — | `g15s` |
> | `total_hits_ssg08` | — | `g15h` |
> | `total_kills_ssg08` | — | `g15k` |
> | `total_shots_taser` | — | `g35s` |
> | `total_kills_taser` | — | `g35k` |

If you noticed, this only has five defuse maps. That's because CS: GO doesn't track rounds from Mirage, Overpass, Cache, Canals, and all operation maps. Also the SG553 is called the SG**556** in the API. Why? Who knows. I swear the CS: GO devs where forced to make this back before Cache existed and have so far refused to update it. What I would give for them to just spend 2 minutes adding all the new maps to the API.

## Limited Tracking
For casual players that don't want to create an account, we still offer some basic tracking. Most other sites, actually every site I've seen, just shows today's cached stats, and that's it. We do more than that. For this section, since we'll be contrasting limited and user tracking, I'll just refer to it as limited accounts and user accounts.

By signing in through Steam on the website, you instantly create a limited account. This saves up to 30 entries of history stats and limited grand stats. History tracking will track all the following stats from the table above. Grand stats are limited to only the below stats.

| Limited Grand Stats |
| :---: |
| `kills` `deaths` `headshots` `plants` `defuse` `hostage` `rounds` `rwins`<br/>`matches` `mwins` `shots` `hits` `damage` `money` `contrib` `mvps` `time` |

For more information about History and Grand stats, keep scrolling.

## User Tracking
Players who signup for a CSGO Skill account with a username and email unlock full user tracking. For History stats, it tracks a significantly longer period of time! As for Grand stats, it tracks everything detailed in the [Grand Stats](#grand-stats) section below! Continue reading to learn more about what History tracking is.

## Current Stats
Current stats is exactly what it sounds like. Every day or more, the current stats can be updated. There are two parts, the `now` and the `recent`. Every tracking day the site updates the `now` with every stat in the conversion table. If someone browses to a profile, it has the chance to update the `recent` stats. For the recent to update, it has to be over an hour since the last update and the person must manually click a button that says something like "refresh."

However, if an account was skipped for tracking that day, the site will show some text like "Last updated on XX/XX/XXXX" and the person must still click the "refresh" button to update the `recent` stats. This does not update the `now` however. By default, the website only shows the `recent` stats when asked. The `now` is only used for tracking daily changes for History stats, and can only be updated when an account gets a daily stats track. More detail is in the [History Stats](#history-stats) section.

## Grand Stats
Grand stats track the total number of certain stats since the account's inception into tracking. Regardless if you sign in with 10 kills or 10 thousand kills, everyone restarts at 0 for Grand stats. This is mostly for our own bragging since we could say something like "Over 10 billion kills tracked so far" which would honestly be really f***ing amazing if the site collectively logged 10 billion kills.

Anyway here are the exact stats that Grand stats is responsible for. Limited accounts get a fraction of these, see [Limited Tracking](#limited-tracking) above.

| Grand Stats |
| :---: |
| `time` `kills` `deaths` `headshots` `plants` `defuse` `hostage` `rounds`<br/>`rwins` `matches` `mwins` `shots` `hits` `damage` `money` `contrib` `mvps`<br/>`knife` `nades` `fires` `flash` `doms` `revs` `overkill` `pistolwin` `donation`<br/>`wins_cbble` `wins_dust2` `wins_inferno` `wins_nuke` `wins_train`<br/>`rnds_cbble` `rnds_dust2` `rnds_inferno` `rnds_nuke` `rnds_train` |

This is done at the same time as history tracking. It just compares the changes for any stats here and adds the difference onto the current value. I've decided not to save the current stats as an offset for calculating grand stats, because then it would sacrifice the ability to actually track them.

## History Stats
History stats are daily increases in stats which are saved over a very long time. Limited accounts get 30 stat entries, but user accounts get tracking indefinitely! The method isn't very straight forward though. History stats are generated by combining yesterday's `now` from Current stats and today's CS: GO API response. For account deemed active or worth tracking, the site will log those changes into the Grand and History stats.

It starts by pulling the current CSGO API stats, and pulls the current `now` stats, which represent yesterday's beginning stats. It then updates the current `now` and `recent` to be the API response for the day, and logs the current time with it. After it has the list of yesterday's and today's stats, it builds a list of the differences for everything that changed. If the player had played the previous day, it should show up as changes in time played, kills, and deaths at least. If no changes are found, it simply saves the current stats and continues to the next account.

If changes are found however, it adds the differences to the related Grand stats, and saves them as a daily entry in the History stats. As other daily entries become older than 3 months, it gradually combines them into a single monthly entry and disposes of the daily one. This gives users daily changes for 3 months, and monthly changes indefinitely.

## Mega Stats
A site dedicated to tracking stats MUST also show it's own stats. I have a very particular set of documents, documents that I have acquired over a very long career, documents that make stats a nightmare for the average website. Well, actually just one document.

```js
// The stats tracked here should be fairly obvious. They are updated daily at
// the end of all account tracking
{
    "steam_id":"mega",
    "start":1521697058826,
    "kills":"0",
    "deaths":"0",
    "heads":"0",
    "time":"0",
    // ... you get it
}
```
As you can see, numbers are stored as strings. This is because I expect some of them to get very big in the future, and expecting large numbers now is better than implementing them later. We'll use a Node.js library called `bignumber.js`, and once we are ready to store them, we convert them to strings just to prevent any surprises. Since `bignumber` gladly creates and takes strings as initializers, that's what we'll use.

Because it's possible to spoof stats in CS: GO, we need to ensure our mega stats don't get borked by some cleaver beaver discovering how to get billions and millions kills in a few seconds. Don't know how easy it is to spoof stats, but I've definitely seen it before. I saw someone who managed to get a few million windows broken on Office by hacking or something, they claimed they did so and the stats didn't lie. I also know for a fact that achievement maps exist which can get you that stupid "King of the Kill" achievement or "SAR Czar" in a matter of minutes.

For individuals, we will save whatever difference the stats say, even if it's -1000 kills or something. But for Mega stats, we'll apply some filters to each user's stats and then add them on. The table below lists each stat and it's limits.

> | Stat | Limit | Explanation |
> | :---: | :---: | :---: |
> | `kills` | `time` / 6 | 10 kills per minute
> | `deaths` | `time` / 6 | 10 deaths per minute
> | `time` | 86400 | 1 day in round time |
> | `rwins` | min(`time` / 30, `rounds`)  | Smallest of 1 win per 30 seconds or `rounds` played |
> | `rounds` | `time` / 30 | 1 round per 30 seconds |
> | `mwins` | min(`time` / 30, `matches`) | Smallest of 1 win per 30 seconds or `matches` played |
> | `matches` | `time` / 30 | 1 match per 30 seconds |
> | `shots` | `time` * 12.5 | Avg. sustained 750RPM |
> | `hits` | `shots` | Ignoring bullet penetration |
> | `damage` | `kills` * 300 | Somewhat arbitrary |
> | `plants` | `rounds` | Impossible to plant more than once per round |
> | `defuse` | `rwins` | Impossible to defuse and not also win |
> | `hostage` | `rwins` | Impossible to rescue and not also win. Ignores multiple rescues per round intentionally |
> | `contrib` | `time` / 6 | 10 score per minute. Somewhat arbitrary |
> | `money` | `time` * 50 | $3,000 per minute. Somewhat arbitrary |
> | `mvps` | `rwins` | Impossible to get MVP for lost rounds |
> | `knife` | `kills` | Impossible to get more knife kills than `kills` |
> | `nades` | `kills` | Impossible to get more nade kills than `kills` |
> | `fires` | `kills` | — |
> | `flash` | `kills` | — |
> | `csnipe` | `kills` | — |
> | `backfire` | `kills` | — |
> | `doms` | `kills` / 4 | It takes at least 4 consecutive kills to dominate and enemy |
> | `revs` | `kills` / 4 | Somewhat arbitrary |
> | `overkill` | `kills` - `doms` | Must dominate first before overkilling |
> | `pistolwin` | `rwins` | Cannot win more rounds than round wins |
> | `donation` | `rounds` * 5 | Somewhat arbitrary |
> | `wins_cbble` | min(`rnds_cbble`, `rwins`) | Smallest of rounds on Cobblestone or rounds won |
> | `wins_dust2` | min(`rnds_dust2`, `rwins`) | Smallest of rounds on Dust 2 or rounds won |
> | `wins_inferno` | min(`rnds_inferno`, `rwins`) | — |
> | `wins_nuke` | min(`rnds_nuke`, `rwins`) | — |
> | `wins_train` | min(`rnds_train`, `rwins`) | — |
> | `rnds_cbble` | `rounds` | Impossible to play more rounds than rounds played |
> | `rnds_dust2` | `rounds` | Impossible to play more rounds than rounds played |
> | `rnds_inferno` | `rounds` | — |
> | `rnds_nuke` | `rounds` | — |
> | `rnds_train` | `rounds` | — |
> | `heads` | `kills` | Cannot get more headshot kills than `kills` |
> | `g8s` | min(`time` * (264 / 60), `shots`) | Avg. sustained 264RPM. Somewhat arbitrary |
> | `g8h` | `g8s` | Cannot hit more than shots fired. Ignores bullet penetration intentionally. |
> | `g8k` | `kills` | Cannot get more kills than `kills` |
> | `g3s` | min(`time` * (450 / 60), `shots`) | Avg. sustained 450RPM. Somewhat arbitrary |
> | `g3h` | `g3s` | Cannot hit more than shots fired. Ignores bullet penetration intentionally. |
> | `g3k` | `kills` | Cannot get more kills than `kills` |
> | `g6s` | min(`time` * (450 / 60), `shots`) | — |
> | `g6h` | `g6s` | — |
> | `g6k` | `kills` | — |
> | **Notice:** |  | For the rest of the gun stats, follow the current pattern. The RPM limit will be provided instead of the formula from here on |
> | `g0s` | 400RPM | — |
> | `g0h` | — | — |
> | `g0k` | — | — |
> | `g12s` | 550RPM | — |
> | `g12h` | — | — |
> | `g12k` | — | — |
> | `g17s` | 600RPM | — |
> | `g17h` | — | — |
> | `g17k` | — | — |
> | `g18s` | 30RPM | — |
> | `g18h` | — | — |
> | `g18k` | — | — |
> | `g11s` | 600RPM | — |
> | `g11h` | — | — |
> | `g11k` | — | — |
> | `g19s` | 200RPM | — |
> | `g19h` | — | — |
> | `g19k` | — | — |
> | `g10s` | 600RPM | — |
> | `g10h` | — | — |
> | `g10k` | — | — |
> | `g31s` | 700RPM | — |
> | `g31h` | — | — |
> | `g31k` | — | — |
> | `g14s` | 600RPM | — |
> | `g14h` | — | — |
> | `g14k` | — | — |
> | `g21s` | 750RPM | — |
> | `g21h` | — | — |
> | `g21k` | — | — |
> | `g25s` | 750RPM | — |
> | `g25h` | — | — |
> | `g25k` | — | — |
> | `g24s` | 600RPM | — |
> | `g24h` | — | — |
> | `g24k` | — | — |
> | `g28s` | 100RPM | — |
> | `g28h` | — | — |
> | `g28k` | — | — |
> | `g26s` | 700RPM | — |
> | `g26h` | — | — |
> | `g26k` | — | — |
> | `g30s` | 50RPM | — |
> | `g30h` | — | — |
> | `g30k` | — | — |
> | `g32s` | 700RPM | — |
> | `g32h` | — | — |
> | `g32k` | — | — |
> | `g29s` | 50RPM | — |
> | `g29h` | — | — |
> | `g29k` | — | — |
> | `g5s` | 450RPM | — |
> | `g5h` | — | — |
> | `g5k` | — | — |
> | `g2s` | 300RPM | — |
> | `g2h` | — | — |
> | `g2k` | — | — |
> | `g23s` | 700RPM | — |
> | `g23h` | — | — |
> | `g23k` | — | — |
> | `g22s` | 750RPM | — |
> | `g22h` | — | — |
> | `g22k` | — | — |
> | `g27s` | 50RPM | — |
> | `g27h` | — | — |
> | `g27k` | — | — |
> | `g4s` | 350RPM | — |
> | `g4h` | — | — |
> | `g4k` | — | — |
> | `g20s` | 200RPM | — |
> | `g20h` | — | — |
> | `g20k` | — | — |
> | `g16s` | 600RPM | — |
> | `g16h` | — | — |
> | `g16k` | — | — |
> | `g15s` | 40RPM | — |
> | `g15h` | — | — |
> | `g15k` | — | — |
> | `g35s` | 2RPM | — |
> | `g35k` | min(`g35s`, `shots`) | You cannot have more Zeus kills than shots |

After some testing, I've determined that these filters need to be applied per account and then added on. We can't keep a running total and apply them later. A simple thought experiment proves this. Player A gets 100 kills and no backfires. Player B gets 0 kills but 100 backfires. Clearly that total cannot be used since it is obviously not correct. By using a running total, we'll only see 100 kills and 100 backfires, which is perfectly valid, although it actually isn't. Because of this we must apply all filters before adding it to our Mega stats.


## Example Current

```js
{
    "now":{
        "updated":1522450802062,
        "kills":40357,
        "deaths":31411,
        "time":1632608,
        "plants":1008,
        "defuse":281,
        "rwins":17077,
        "damage":5677296,
        "money":59543008,
        "hostage":501,
        "knife":596,
        "nades":140,
        "doms":1229,
        "overkill":1536,
        "revs":412,
        "shots":672435,
        "hits":139240,
        "g0k":1007,
        "g0s":25782,
        "g0h":5018,
        // ...
    },
    "recent":{
        "updated":1522450802062,
        // ...
    }
}
```

## Example Grand

```js
{
    "kills":559,
    "deaths":422,
    "time":27003,
    "plants":87,
    "defuse":24,
    "rwins":143,
    "damage":78639,
    "money":543560,
    "hostage":0,
    "knife":7,
    "nades":1,
    "doms":34,
    "overkill":17,
    "revs":8,
    "shots":5345,
    "hits":1242,
    // ...
}
```

## Example History

```js
{
    "daily":[
        {
            // Excerpt from my own history
            "date": Date("2018-03-24 18:00:00.000"),
            "time":3253,
            "kills":113,
            "deaths":81,
            "damage":15461,
            "rounds":7,
            "rwins":6,
            "doms":7,
            "overkill":9,
            "shots":1699,
            // ...
        },
        {
            // Last played 3 days ago
            "date": Date("2018-03-21 18:00:00.000"),
            // ...
        },
        // ...
    ],
    "monthly": [
        {
            "date":"2017-11",
            // ...
        }
    ]
}
```
<br/><br/><br/>And that's basically it<br/><br/>

**v0.1-alpha**
# Introduction
Welcome to the hub for all that is database storage and tracking. Everything here is OFFICIAL, meaning it is the way the project currently works in its most recent version. Other versions will be stored in separate pages as they become outdated. Currently there isn't even a release yet, so everything here is kinda unstable. However, it will probably not change much since this page is already very refined, and most systems already use everything as described here.

# Table of Contents
> [**Introduction**](#introduction)

> [**Table of Contents**](#table-of-contents)

> [**DB Structure**](#db-structure) - Basic information about databases

> [**Basic Tracking**](#tracking) - Basic information about tracking

> [**Website**](#website) - Information about the website structure
>> [**Players**](#collection-players) - The `Players` collection structure<br/>
>> [**Stats**](#collection-stats) - The `Stats` collection structure<br/>
>> [**Secret**](#secret-algorithm) - The method for generating secret hashes<br/>
>> [**Usage**](#usage-of-the-secret) - How the Secret is used<br/>
>> [**Sessions**](#web-sessions) - Details on web sessions

> [**App**](#app) - Information about the app structure
>> [**Accounts**](#table-accounts) - The `accounts` table structure

> [**Validation**](#validation) - How information like usernames and emails are validated
>> [**Emails**](#emails) - The requirements for email addresses<br/>
>> [**Usernames**](#usernames) - Formation of usernames from personas


> [**Tracking**](#tracking-1) - Detailed tracking information
>> [**Stats Table**](#stats-table) - Conversion table for stats (Steam API => Skill API)<br/>
>> [**Limited Tracking**](#limited-tracking) - How stat tracking works on limited accounts<br/>
>> [**User Tracking**](#user-tracking) - How stat tracking works on user accounts<br/><br/>
>> [**Current Stats**](#current-stats) - Details of `current` stats in DB<br/>
>> [**Grand Stats**](#grand-stats) - Details of `grand` stats in DB<br/>
>> [**History Stats**](#history-stats) - Details of `history` stats in DB<br/>
>> [**Mega Stats**](#mega-stats) - Details for site-wide stats in DB<br/><br/>
>> [**Example Current**](#example-current) - Example document for current stats<br/>
>> [**Example Grand**](#example-grand) - Example document for grand stats<br/>
>> [**Example History**](#example-history) - Example document for history stats<br/>


# DB Structure
Website: MongoDB<br/>App: Built-in SQLite Database (android)<br/>Communication: Strictly JSON

# Tracking
The website itself is where ALL the tracking happens. While we could save time and resources by having the app do all the heavy-lifting like making the API request, parsing it, saving the important bits, and then sending it to us, that would simply make spoofing stats ridiculously easy. Originally that is how I designed it, long before this repository existed, but I realized it would be far more consistent and "secure" to do all that on the server.

There is only one time that we really care about, the start of each day. At exactly 00:00-UTC, we update all users stats and save for the previous day. For example, a user living in Moscow will see their Feb. 14th stats at the earliest 03:00-Feb. 15th local time. A user in New York will see their Feb. 14th stats at the earliest 19:00-Feb. 14th local time.

In future versions, the user will be asked to select a timezone that best fits them, so that they can see their daily stats on the proper dates.

# Website
The website runs on Node.js and uses MongoDB for storage. The site is hosted by Heroku. The database is hosted by the official MongoDB website, although the service is through Amazon or something like that. The actual website is a single script that serves up html documents and handles API things, so it is constantly running and listening for requests. It runs other scripts at specific intervals, which handle things like stat updates, old account removal, etc.

In short, `server.js` listens for requests, and launches other scripts when the time comes.

This project uses MongoDB for all it's data storage. Previously it used a MySQL database and literally only stored JSON text, so converting to MongoDB was an obvious choice. Go JSON!

If you're new to this, collections are identical to tables in your normal SQL databases. However, collections just store JSON documents. It's like having a single column table in SQL. Anyway, below are the collections that we use. To make things simpler, all documents for a collection follow very strict formats.

## Collection: `Players`
This collection stores basic profile information about all players who have an account. It does not include players who simply add themselves to basic stat tracking.

```javascript
/* Integer and unique identifier. Never shown publicly. For security reasons,
   this is actually offset by a random number, so it doesn't start at 0. */
id: 0

/* The currently connected Steam account ID, unique */
steamid: "0123456789"

/* Custom username, lowercased, URL friendly, unique */
username: "almic"

/* Custom persona name, whatever the user wants */
persona: "Almic"

/* 200 character, html encoded description of profile, written by the player.
   Because some things may be html encoded, the resulting length can be longer
   than 200 characters */
bio: "Plays CS: GO"

/* Unique email address, mostly for spam reduction */
email: "devteam@csgo-skill.com"

/* Unique hash generated for every user. Algorithm is further down the page */
secret: "0-40c788a04-9609163eeae-02215b0b735664-a7c1e181d552836788d42ab0-b8..."

/* Used to remove old and unverified accounts, and bragging rights */
created: "2018-02-15T00:04:53Z" // Always YYYY-MM-DD'T'HH:MM:SS'Z'

/* Simpler way to check verification status of an account, 0: No, 1: YES */
verified: 1

/* A special JSON object for holding various account status information.
   Typically this is just a middleman for storing temporary data that some
   scripts will look at later. As such, most stuff will start with a prefix
   of the script that will use it, others are more global, like 'level' */
status: {"level": "admin"}
```

Here's a table for some more specific details about each key.

| Key | Value | Properties |
| :---: | :---: | :---: |
| `id` | Int | Unique, Auto-increment |
| `steamid` | String | Unique, Max 20 length[*](#validation) |
| `username` | String | Unique, url-encoded[*](#username-validation), Max 35 length |
| `persona` | String | UTF-8, Max 35 length |
| `bio` | String | HTML encoded, Max 200 length before encode |
| `email` | String | Unique, Valid[*](#emails), Max 50 length |
| `secret` | String | Unique[*](#secret-algorithm) |
| `created` | String | Date-Time like `YYYY-MM-DD'T'HH:MM:SS'Z'` |
| `verified` | Boolean | --- |
| `status` | Object | Various other keys used by backend |


## Collection: `Stats`
This collection is where all the bulk of the information is. This is where all the stats history is saved for everyone, and some other extra stats.

```javascript
/* Steam ID for the user stats that are stored here. NEVER changes once set! */
steamid: "0123456789"

/* Helps the site determine how to track stats for this Steam ID */
user: true

/* Holds some basic info about the last 10 visits a profile received */
visits: [
    {"user":"anon","date":"2018-02-15T19:34:11Z"},
    {"user":"012345689","date":"2018-02-14T11:25:08Z"}
]

/* Used to determine if a user account is "inactive" */
active: "2018-01-30T21:41:39Z" // Always YYYY-MM-DD'T'HH:MM:SS'Z'

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
| `steamid` | String | Unique, Max 20 length[*](#validation) |
| `user` | Boolean | --- |
| `visits` | Array | Max 20 length, format as above |
| `active` | String | Date-Time like `YYYY-MM-DD'T'HH:MM:SS'Z'` |
| `current` | Object | [Tracking: `current`](#current) |
| `grand` | Object | [Tracking: `grand`](#grand) |
| `history` | Object | [Tracking: `history`](#history) |

## Secret Algorithm
The algorithm itself isn't secret, but what it creates is. Here are two separate implementations for historic purposes. This was originally written in PHP, but converted to Node.js now.

### Node.js
> ```javascript
> const crypto = require('crypto')
>
> function randNum(min, max) {
>
>     // Some credit to https://github.com/joepie91/node-random-number-csprng
>     /* Careful! This doesn't work with large ranges. Specifically, don't use
>      * this with ranges larger than 2^32 - 1 */
>
>     const range = max - min
>     if (range >= Math.pow(2, 32))
>         console.log("Warning! Range is too large.")
>     
>     var tmp = range
>     var bitsNeeded = 0
>     var bytesNeeded = 0
>     var mask = 1
>
>     while (tmp > 0) {
>         if (bitsNeeded % 8 === 0) bytesNeeded += 1
>         bitsNeeded += 1
>         mask = mask << 1 | 1
>         tmp = tmp >>> 1
>     }
>     const randomBytes = crypto.randomBytes(bytesNeeded)
>     var randomValue = 0
>
>     for (var i = 0; i < bytesNeeded; i++) {
>         randomValue |= randomBytes[i] << 8 * i
>     }
>
>     randomValue = randomValue & mask;
>
>     if (randomValue <= range) {
>         return min + randomValue
>     } else {
>         return randNum(min, max)
>     }
> }
>
> const result = 0
> var hash = crypto.createHash('sha512')
> hash.update(crypto.randomBytes(256))
> var string = hash.digest('hex')
>
> /* this adds random dashes in the secret, to make it slightly more
>    secure and effectively make it impossible to guess */
> var first = 0
> var next = randNum(8,12)
> var secret = string.substr(0, next)
> var o_secret = secret
>
> while (o_secret.length !== string.length) {
>     first += next
>     next = randNum(6,24)
>     secret += '-' + string.substr(first, next)
>     o_secret += string.substr(first, next)
> }
>
> secret = result + '-' + secret
> ```

### PHP [OLD]
> ```php
> $result = $result["iid"]; // The Auto-Increment `id`
> $string = hash("sha512", random_bytes(200));
>
> /* this adds random dashes in the hash id, to make it slightly more
>    secure and effectively make it impossible to guess a user's id */
> $first = 0;
> $next = random_int(8,12);
> $hash_id = substr($string, 0, $next);
> $o_string = $hash_id;
>
> while (strlen($o_string) != strlen($string)){
>     $first += $next;
>     $next = random_int(6,24);
>     $hash_id .= "-" . substr($string, $first, $next);
>     $o_string .= substr($string, $first, $next);
> }
>
> $hash_id = $result . "-" . $hash_id; // This is 100% unique all the time
> ```
> Wow this is so much shorter. Why doesn't the crypto library have a good random number generator?

## Usage of the Secret
The reason for such a secure hash is so that the App can talk to the Server and get information about an account. The security of the hash is apparent, so the full security is now in the hands of the server and the user's device. Currently nothing very sensitive is saved to accounts, only an email address. If a hacker really wanted email addresses, they might have a better chance somewhere else than trying to brute force secrets for one email at a time.

With a secret, the App can request the email address associated with the account, so the user can quickly see the address or update it from the device. Since the email is ONLY used to reduce spam and MAYBE send occasional updates, should the user subscribe to it, nothing is lost if a malicious attacker changes the email on an account. Because of this, the email CANNOT be used to verify the identity of a user. Their Steam account is the best proof, however I don't know what we'd be *proving* anyway since the site just tracks stats for Steam accounts and shows them publicly anyway.

It can also be used to change account settings from the App.

**TL;DR** - The secret is only for displaying the email connected to an account on the App, and for starting the process of updating it from the App. And for changing account settings.

## Web Sessions
Since users can log in to the website, there is more need for a temporary session token than a secret. And since the secret is pretty secret, we'll just generate a temporary cookie on the browser and save a random token to it as well as on our server. We'll put it in the `status` object, and it'll look like this:

```javascript
status: {"session":{"token":"abcdefg","created":123456789}}
```
As you can see, the token is saved with a timestamp, so we know when to stop accepting that token and request the user to log back in.

# App
The app is currently only available for Android, but I definitely want to make it for iOS in the future. The app uses the built-in library `android.database.sqlite.SQLiteDatabase`, or SQLite for short, to save basic account data. For the moment, the app needs an active internet connection to show stat history, but grand stats and current stats are cached when possible. I plan to cache stat history in the future when I can work out an ethical way to store so much data.

I also have plans for an offline experience, where a user can use the app without connecting an account and access the things that wouldn't require an account. It would also be nice for users to connect a Steam account without signing up for a CSGO Skill account, allowing them faster access to limited tracking.

## Table: `accounts`
This is currently the only table, and it stores just enough information to identify the user online and offline. It also stores a bit of stat information so if they lose internet, they can still access some of the most recent stat information.

```javascript
// Yeah this is how I'm going to represent an SQL table.

/* Steam ID for the user, unique */
id: "0123456789"

/* Last synced value of the 'persona' stored on the website */
name: "Almic"

/* Last synced value of the 'username' stored on the website */
url: "almic"

/* The email address associated with the account */
email: "devteam@csgo-skill.com"

/* User's secret hash, used when communicating with the server */
secret: "0-40c788a04-9609163eeae-02215b0b735664-a7c1e181d552836788d42ab0-b8..."

/* A link to the user's full Steam avatar */
avatar: "https://steamcdn-a.akamaihd.net/steamcommunity/public/images..."

/* Current account status info, synced every launch of the app */
status: {...}

/* Last synced stat info from the server, updated daily when the user opens
   the app, or more if the user makes more requests to the server */
data: {...}

/* Info about the last tasks given to the user to prevent doubling or
   excessive task selections */
tasks: {...}
```

Here's a table for some more specific details about each column.

| Column | Type | Properties |
| :---: | :---: | :---: |
| `id` | TEXT | Unique |
| `name` | TEXT | --- |
| `url` | TEXT | --- |
| `email` | TEXT | --- |
| `secret` | TEXT | --- |
| `avatar` | TEXT | --- |
| `status` | TEXT | --- |
| `data` | TEXT | --- |
| `tasks` | TEXT | --- |

No validation needs to be done for user data like name or email because the server will do that for us anyways. Since all data is given to the app by the server, we can assume data is good to store, since it should match what the server has at that time.

# Validation
User info is validated using some pretty specific rules. Even the Steam ID needs to meet some rules, despite coming directly from Steam. Some of these are redundant, but I think closing a hole is better than assuming it doesn't exist.

Here's a table of the things the user can provide us, and some basic rules. If you want more information, some of them have their own detailed sections on why the rules were chosen.

| Name | Rules |
| :---: | :---: |
| `steamid` | 15 - 20 characters, all numbers, unique. |
| `email` | 4-50 characters, must have at least one `@`<br/>character, unique. |
| `secret` | 135-160 characters, `a-z` `0-9` `-` only, unique. |
| `persona` | 3-35 characters. HTML encoded afterwards |
| `username` | 3-35[39][*](username-creation) characters, cannot have HTML encoded<br/>characters, unique. |
| `bio` | 1-200 characters. HTML encoded afterwards. |

These are the primary things we ask the user or the device for frequently, and are the most important parts of account creation. Before we ever shove them into a collection or search for them, we must match them all against these rules. It reduces security holes and keeps the database resources free from queries that would obviously fail.

## Email Validation
Emails are the start of spam reduction, only to prevent the creation of spam accounts or the use of multiple accounts to one email. In the end, we use the email only to send out newletters if the user asked for it, and absolutely nothing else.

From the great words of this [StackOverflow comment](https://stackoverflow.com/questions/46155/how-can-an-email-address-be-validated-in-javascript#comment32449168_46181):
> ***You*** cannot validate email addresses, period!! The only one who can validate an email address is the ***provider*** of the email address. So why would you try? For example, this answer says these email addresses: `%2@gmail.com, "%2"@gmail.com, "a..b"@gmail.com, "a_b"@gmail.com, _@gmail.com, 1@gmail.com , 1_example@something.gmail.com` are all valid, but Gmail will ***never*** allow any of these email addresses. You should do this by accepting the email address and sending an email message to that email address, with a code/link the user must visit to confirm validity.

Simply put, we shouldn't try to validate emails ourselves. Whatever the user gives us, we'll try and email it the link. If they gave us a valid email, then they'll be able to see our message and verify the account. But because we need to set limits on what we'll take, it needs to have at least an `@` character somewhere, be at least 4 characters long, and be 50 characters or shorter. Anything else is, to my knowledge, ridiculous and unlikely an email that a real person would use.

## Username Validation
When it comes to the persona, the only limit is 3-35 characters. However the username is used for URL linking, so it clearly needs to be strict. Currently the best way to go is to copy+paste the persona name when a user signs up, convert it to lowercase, and remove these specific characters.

| Removed |
| :---: |
| `.` `~` ` ` `:` `/` `\`<br/>`?` `&` `!` `#` `[]`<br/>`@` `$` `'` `"` `()`<br/>`*` `+` `,` `;` `=` `%`<br/>`^` `{}` ``|`` `` ` `` `<>` |

The idea is to allow any sort of language characters like 'こんにちは' or 'Прощай', which can be used in URLs. By converting it to lower case, we can prevent people from impersonating others, like with Hello (`Hello`) and HeIlo (`HeIlo`). It also removes the worry, if someone was typing the link, to type the exact capitalization. But what do we do if someone named `Ethan`, which would reduce to `ethan` automatically, and another named `[ETHAN!]`, which also reduces to `ethan`, create an account?

### Username Creation
Although we allow people to use whatever persona they desire, the username must be unique. Discord solves the issue by appending an id number to names that are repeated. Anyone can call themselves `Ethan`, but there will be only one `Ethan#0001`. We'll do the same, sort of. If someone creates an account with a persona name that, after transforming, is identical to another username, we'll add some number to the end of it. Literally just slap it on the end.

This, as a result, would allow accounts with usernames longer than the maximum limit. For these cases the max length is internally extended to 39, allowing for 4 numbers to provide unique usernames. Users still cannot pick names longer than 35, so we'd need a 35 length username to be repeated to push the limit. Currently this *should absolutely* give enough usages (10000) per repeated username. As users change their name, those 10000 repeats will get freed up and used by others.

In the case that a repeated username exceeds 10000 concurrent uses, then we'll just tell the user "Pick another name." That would be really sad to do, but it removes the need to worry about cropping names for more numbers and overlapping into a whole different username with a different number of available repeats etc., etc. Regardless, the same exact username being used 10,001 times would be absolutely ridiculous and I doubt this will ever have enough users to reach that number. *MAYBE* a username will hit a thousand repeats, but beyond 10k is insane.

To check for the next available name, just start by looking up accounts with usernames that start with the same character sequence and only have numbers afterwards, then sorting them alphabetically and filling the first gap in numbers it finds. Regex: `^{username}[0-9]*$`

**TL;DR**: For repeated usernames, append an increasing number from 1-9999 and make sure it's unique.

# Tracking
The biggest aspect of the project is in depth tracking for every statistic possible for every individual player. Lucky for us, recording numbers doesn't really take up much space, especially with the method we have for doing so. This section will probably take up way more of the page than it should, but that's only because the examples here are complete.

### Visits
Earlier you may have noticed the `visits` key in the [Stats Collection](#collection-stats) section. This offers a way for us to skip tracking for accounts which are not active. In general, if we don't see any changes in stats for an account, we completely skip recording it in the collection. However we still consume precious time requesting the CSGO stats and comparing for changes.

By tracking how often the stats for an account are actually looked at, we can skip the check completely since no one seems to care anyway. The choices are somewhat arbitrary, but I think they make sense. Every time a profile is requested on the site, we log an anonymous visit and record the date. If a logged in account requests a profile's stats, it records the user's Steam ID. If the owner of the account logs into the website, we update the `active` key with the current timestamp.

This provides us 2 types of visits, anonymous and user visits, as well as the last time the owner logged in. The visits array gives us the 20 most recent visits for an account. This helps us determine if anyone would care that we stopped tracking stats for a few days. Again, the choices are somewhat arbitrary but they make sense to me. The first number is the minimum for limited accounts, and the second in [brackets] is for user accounts.

- First, the site checks if the `active` date is within the last 3 [4] weeks. If it is, it tracks stats for the day. If not, continue.
- Second, the site looks to see if any user visits occurred in the last 2 [3] weeks. If there are, it tracks stats for the day. If not, continue.
- Last, the site counts the number of anonymous visits. If it sees 2 [1] which occurred in the last week, it tracks stats for the day. If not, it skips the account completely for the day.

This is simply to relieve the load on our servers while it goes account-by-account. It also reduces the storage by abandoning dead accounts or accounts that don't actively use the site but were registered in the past. For user accounts, we are more lenient on the visits. Also, opening the app with a signed in account also updates the `active` timestamp if internet is available.

## Stat Conversion
You may find the table below very helpful, as it offers shorter key names and their conversion to the stat in the CSGO API.

> | Name | Description | New Name |
> | :--- |    :---:    | :---     |
> | `total_kills` | - | `kills` |
> | `total_deaths` | - | `deaths` |
> | `total_time_played` | In round seconds | `time` |
> | `total_wins` | **R**ound **wins** | `rwins` |
> | `total_rounds_played` | - | `rounds` |
> | `total_matches_won` | **M**atch **wins** | `mwins` |
> | `total_matches_played` | - | `matches` |
> | `total_shots_fired` | - | `shots` |
> | `total_shots_hit` | - | `hits` |
> | `total_damage_done` | - | `damage` |
> | `total_planted_bombs` | - | `plants` |
> | `total_defused_bombs` | - | `defuse` |
> | `total_rescued_hostages` | - | `hostage` |
> | `total_contribution_score` | - | `contrib` |
> | `total_money_earned` | - | `money` |
> | `total_mvps` | - | `mvps` |
> | `total_kills_knife` | - | `knife` |
> | `total_kills_hegrenade` | - | `nades` |
> | `total_kills_molotov` | - | `fires` |
> | `total_kills_against_zoomed_sniper` | - | `csnipe` |
> | `total_kills_enemy_weapon` | - | `backfire` |
> | `total_dominations` | - | `doms` |
> | `total_revenges` | - | `revs` |
> | `total_domination_overkills` | - | `overkill` |
> | `total_wins_pistolround` | - | `pistolwin` |
> | `total_weapons_donated` | - | `donation` |
> | `total_wins_map_de_cbble` | - | `wins_cbble` |
> | `total_wins_map_de_dust2` | - | `wins_dust2` |
> | `total_wins_map_de_inferno` | - | `wins_inferno` |
> | `total_wins_map_de_nuke` | - | `wins_nuke` |
> | `total_wins_map_de_train` | - | `wins_train` |
> | `total_rounds_map_de_cbble` | - | `rnds_cbble` |
> | `total_rounds_map_de_dust2` | - | `rnds_dust2` |
> | `total_rounds_map_de_inferno` | - | `rnds_inferno` |
> | `total_rounds_map_de_nuke` | - | `rnds_nuke` |
> | `total_rounds_map_de_train` | - | `rnds_train` |
> | `total_kills_headshot` | - | `heads` |
> | `total_shots_deagle` | Includes R8 | `g8s` |
> | `total_hits_deagle` | - | `g8h` |
> | `total_kills_deagle` | - | `g8k` |
> | `total_shots_elite` | Dual Berettas | `g3s` |
> | `total_hits_elite` | - | `g3h` |
> | `total_kills_elite` | - | `g3k` |
> | `total_shots_fiveseven` | Includes CZ75 | `g6s` |
> | `total_hits_fiveseven` | - | `g6h` |
> | `total_kills_fiveseven` | - | `g6k` |
> | `total_shots_glock` | - | `g0s` |
> | `total_hits_glock` | - | `g0h` |
> | `total_kills_glock` | - | `g0k` |
> | `total_shots_ak47` | - | `g12s` |
> | `total_hits_ak47` | - | `g12h` |
> | `total_kills_ak47` | - | `g12k` |
> | `total_shots_aug` | - | `g17s` |
> | `total_hits_aug` | - | `g17h` |
> | `total_kills_aug` | - | `g17k` |
> | `total_shots_awp` | - | `g18s` |
> | `total_hits_awp` | - | `g18h` |
> | `total_kills_awp` | - | `g18k` |
> | `total_shots_famas` | - | `g11s` |
> | `total_hits_famas` | - | `g11h` |
> | `total_kills_famas` | - | `g11k` |
> | `total_shots_g3sg1` | - | `g19s` |
> | `total_hits_g3sg1` | - | `g19h` |
> | `total_kills_g3sg1` | - | `g19k` |
> | `total_shots_galilar` | - | `g10s` |
> | `total_hits_galilar` | - | `g10h` |
> | `total_kills_galilar` | - | `g10k` |
> | `total_shots_m249` | - | `g31s` |
> | `total_hits_m249` | - | `g31h` |
> | `total_kills_m249` | - | `g31k` |
> | `total_shots_m4a1` | M4A4 & M4A1-S | `g14s` |
> | `total_hits_m4a1` | - | `g14h` |
> | `total_kills_m4a1` | - | `g14k` |
> | `total_shots_mac10` | - | `g21s` |
> | `total_hits_mac10` | - | `g21h` |
> | `total_kills_mac10` | - | `g21k` |
> | `total_shots_p90` | - | `g25s` |
> | `total_hits_p90` | - | `g25h` |
> | `total_kills_p90` | - | `g25k` |
> | `total_shots_ump45` | - | `g24s` |
> | `total_hits_ump45` | - | `g24h` |
> | `total_kills_ump45` | - | `g24k` |
> | `total_shots_xm1014` | - | `g28s` |
> | `total_hits_xm1014` | - | `g28h` |
> | `total_kills_xm1014` | - | `g28k` |
> | `total_shots_bizon` | - | `g26s` |
> | `total_hits_bizon` | - | `g26h` |
> | `total_kills_bizon` | - | `g26k` |
> | `total_shots_mag7` | - | `g30s` |
> | `total_hits_mag7` | - | `g30h` |
> | `total_kills_mag7` | - | `g30k` |
> | `total_shots_negev` | Laser Beam | `g32s` |
> | `total_hits_negev` | - | `g32h` |
> | `total_kills_negev` | - | `g32k` |
> | `total_shots_sawedoff` | - | `g29s` |
> | `total_hits_sawedoff` | - | `g29h` |
> | `total_kills_sawedoff` | - | `g29k` |
> | `total_shots_tec9` | Includes CZ75 | `g5s` |
> | `total_hits_tec9` | - | `g5h` |
> | `total_kills_tec9` | - | `g5k` |
> | `total_shots_hkp2000` | USP-S & P2000 | `g2s` |
> | `total_hits_hkp2000` | - | `g2h` |
> | `total_kills_hkp2000` | - | `g2k` |
> | `total_shots_mp7` | - | `g23s` |
> | `total_hits_mp7` | - | `g23h` |
> | `total_kills_mp7` | - | `g23k` |
> | `total_shots_mp9` | - | `g22s` |
> | `total_hits_mp9` | - | `g22h` |
> | `total_kills_mp9` | - | `g22k` |
> | `total_shots_nova` | - | `g27s` |
> | `total_hits_nova` | - | `g27h` |
> | `total_kills_nova` | - | `g27k` |
> | `total_shots_p250` | - | `g4s` |
> | `total_hits_p250` | - | `g4h` |
> | `total_kills_p250` | - | `g4k` |
> | `total_shots_scar20` | - | `g20s` |
> | `total_hits_scar20` | - | `g20h` |
> | `total_kills_scar20` | - | `g20k` |
> | `total_shots_sg556` | SG 55**3** | `g16s` |
> | `total_hits_sg556` | - | `g16h` |
> | `total_kills_sg556` | - | `g16k` |
> | `total_shots_ssg08` | - | `g15s` |
> | `total_hits_ssg08` | - | `g15h` |
> | `total_kills_ssg08` | - | `g15k` |
> | `total_shots_taser` | - | `g35s` |
> | `total_kills_taser` | - | `g35k` |

If you noticed, this only has five defuse maps. That's because CGSO doesn't track rounds from Mirage, Overpass, Cache, Canals, and all operation maps. Also the SG553 is called the SG**556** in the API. Why? Who knows. I swear the CSGO devs where forced to make this back before Cache existed and have so far refused to update it. What I would give for them to just spend 2 minutes adding all the new maps to the API.

## Limited Tracking
For casual players that don't want to create an account, we still offer some basic tracking. Most other sites, actually every site I've seen, just shows today's cached stats, and that's it. We do more than that. For this section, since we'll be contrasting limited and user tracking, I'll just refer to it as limited accounts and user accounts.

By signing in through Steam on the website, you instantly create a limited account. This saves one month (30 days) of limited history stats and limited grand stats. History tracking will track all the following stats from the table above.

| Limited History Stats |
| :---: |
| `kills` `deaths` `time` `rwins` `rounds` `mwins` `matches` `shots` `hits`<br/>`damage` `contrib` `money` `mvps` `knife` `doms` `revs` `overkill` `pistolwin`<br/>`wins_cbble` `wins_dust2` `wins_inferno` `wins_nuke` `wins_train`<br/>`rnds_cbble` `rnds_dust2` `rnds_inferno` `rnds_nuke` `rnds_train`<br/>`heads` `g8s` `g8h` `g8k` `g6s` `g6h` `g6k` `g0s` `g0h` `g0k` `g12s` `g12h` `g12k`<br/>`g17s` `g17h` `g17k` `g18s` `g18h` `g18k` `g14s` `g14h` `g14k` `g5s` `g5h` `g5k`<br/>`g2s` `g2h` `g2k` `g4s` `g4h` `g4k` `g16s` `g16h` `g16k` `g15s` `g15h` `g15k` |

In short, it skips plants, defuses, hostage rescues, nade kills, fire kills, counter snipes, backfires, weapon donations, all heavy guns and SMGs, the Galil AR and Famas, the Auto Snipers and Zeus.

| Limited Grand Stats |
| :---: |
| `kills` `deaths` `headshots` `plants` `defuse` `hostage` `rounds` `rwins`<br/>`matches` `mwins` `shots` `hits` `damage` `money` `contrib` `mvps` `time` |

For more information about History and Grand stats, keep scrolling.

## User Tracking
Players who create an account with a username and email unlock full user tracking. For History stats, it tracks everything in the conversion table. It also tracks for a significantly longer period of time! As for Grand stats, it tracks everything detailed in the [Grand Stats](#grand-stats) section below. Continue reading to learn more about what History tracking is.

## Current Stats
Current stats is exactly what it sounds like. Every day or more, the current stats can be updated. There are really two parts, the `now` and the `recent`. Every tracking day the site updates the `now` with every stat in the conversion table. If someone browses to a profile, it has the chance to update the `recent` stats. For the recent to update, it has to be over an hour since the last update and the user must manually click a button that says something like "refresh."

However, if an account was skipped for tracking that day, the site will show some text like "Last updated on XX/XX/XXXX" and the person must still click the "refresh" button to update the `recent` stats. This does not update the `now` however. By default, the website only shows the `recent` stats when asked. The `now` is only used for tracking daily changes for History stats, and can only be updated when an account gets a daily stats track. More detail is in the [History Stats](#history-stats) section.

## Grand Stats
Grand stats track the total number of certain stats since the account's inception into tracking. Regardless if you sign in with 10 kills or 10 thousand kills, everyone restarts at 0 for Grand stats. This is mostly for our own bragging since we could say something like "Over 10 billion kills tracked so far" which would honestly be really fucking amazing if the site collectively logged 10 billion kills.

Anyway here are the exact stats that Grand stats is responsible for. Limited accounts get a fraction of these, see [Limited Tracking](#limited-tracking) above.

| Grand Stats |
| :---: |
| `time` `kills` `deaths` `headshots` `plants` `defuse` `hostage` `rounds`<br/>`rwins` `matches` `mwins` `shots` `hits` `damage` `money` `contrib` `mvps`<br/>`knife` `nades` `fires` `doms` `revs` `overkill` `pistolwin` `donation`<br/>`wins_cbble` `wins_dust2` `wins_inferno` `wins_nuke` `wins_train`<br/>`rnds_cbble` `rnds_dust2` `rnds_inferno` `rnds_nuke` `rnds_train` |

This is done at the same time as history tracking. It just compares the changes for any stats here and adds the difference onto the current value. Yes, we could save the current number and use that as an offset, but then it would also count the times that we hadn't tracked the stats, which is the goal.

## History Stats
History stats are daily increases in stats which are saved over a very long time. Limited accounts get daily stats for 30 days, but user accounts get tracking indefinitely! The method isn't very straight forward though. History stats are generated by combining yesterday's `now` from Current stats and today's CSGO API response. For account deemed active or worth tracking, the site will log yesterday's changes into the Grand and History stats.

It starts by pulling the current CSGO API stats, and pulls the current `now` stats, which represent yesterday's beginning stats. It then updates the current `now` and `recent` to be the API response for the day, and logs the current time with it. After it has the list of yesterday's and today's stats, it builds a list of the differences for everything that changed. If the player had played the previous day, it should show up as changes in time played, kills, and deaths at least. If no changes are found, it simply stops and continues to the next account.

If changes are found however, it adds the differences to the related Grand stats, and saves them as a daily entry in the History stats. As other daily entries become older than 3 months, it gradually combines them into a single monthly entry and disposes of the daily one. This gives users daily changes for 3 months, and monthly changes indefinitely.

## Mega Stats
A site dedicated to tracking stats MUST also show it's own stats. I have a very specific set of documents in the Stats collection, well actually just one. Here it is!

```javascript
// The stats tracked here should be fairly obvious. They are updated daily at
// the end of all account tracking
{
    "steamid":"mega",
    "start":"2018-02-22",
    "kills":"0",
    "deaths":"0",
    "heads":"0",
    "time":"0",
    "plants":"0",
    "defuse":"0",
    "damage":"0",
    "money":"0",
    "hostage":"0",
    "knife":"0",
    "nades":"0",
    "fires":"0",
    "doms":"0",
    "overkill":"0",
    "revs":"0",
    "shots":"0",
    "hits":"0",
    "rounds":"0",
    "rwins":"0",
    "matches":"0",
    "mwins":"0",
    "contrib":"0",
    "mvps":"0",
    "backfire":"0",
    "csnipe":"0",
    "pistolwin":"0",
    "donation":"0",
    "rnds_cbble":"0",
    "rnds_dust2":"0",
    "rnds_inferno":"0",
    "rnds_nuke":"0",
    "rnds_train":"0",
    "wins_cbble":"0",
    "wins_dust2":"0",
    "wins_inferno":"0",
    "wins_nuke":"0",
    "wins_train":"0",
    "g0s":"0",
    "g0h":"0",
    "g0k":"0",
    // ... you get it
}
```
As you can see, numbers are stored as strings. This is because I expect some of them to get very big in the future, and expecting large numbers now is better than implementing them later. We'll use a Node.js library called `bignumber.js`, and once we are ready to store them, we convert them to strings just to prevent any surprises. Since `bignumber` gladly creates and takes strings as initializers, that's what we'll use.

It should go without saying that if, and I don't know how, we encounter a negative difference, then we'll just ignore it and continue on. Wouldn't want some cleaver beaver figuring out that creating a negative difference in their stats will literally subtract and even make our stats negative. It's like going to the bank and trying to transfer -1000 dollars to your friend, thereby withdrawing 1000 from their account. They wouldn't let you do that, would they?

Also, if an individual has a daily stat difference that is absurd, as detailed in the table below, we will limit the stats that get applied. I saw one account which hacked their stats to say they broke over 5 million windows on Office in a matter of minutes. Clearly not possible, absolutely absurd, and definitely not good for us. However, we will log any changes for single accounts, we only filter them for Mega stats.

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
> | `fires` | `kills` | Impossible to get more flame kills than `kills` |
> | `csnipe` | `kills` | --- |
> | `backfire` | `kills` | --- |
> | `doms` | `kills` / 4 | It takes at least 4 consecutive kills to dominate and enemy |
> | `revs` | `kills` / 4 | Somewhat arbitrary |
> | `overkill` | `kills` - `doms` | Must dominate first before overkilling |
> | `pistolwin` | `rwins` | Cannot win more rounds than round wins |
> | `donation` | `rounds` * 5 | Somewhat arbitrary |
> | `wins_cbble` | min(`rnds_cbble`, `rwins`) | Smallest of rounds on Cobblestone or rounds won |
> | `wins_dust2` | min(`rnds_dust2`, `rwins`) | Smallest of rounds on Dust 2 or rounds won |
> | `wins_inferno` | min(`rnds_inferno`, `rwins`) | --- |
> | `wins_nuke` | min(`rnds_nuke`, `rwins`) | --- |
> | `wins_train` | min(`rnds_train`, `rwins`) | --- |
> | `rnds_cbble` | `rounds` | Impossible to play more rounds than rounds played |
> | `rnds_dust2` | `rounds` | Impossible to play more rounds than rounds played |
> | `rnds_inferno` | `rounds` | --- |
> | `rnds_nuke` | `rounds` | --- |
> | `rnds_train` | `rounds` | --- |
> | `heads` | `kills` | Cannot get more headshot kills than `kills` |
> | `g8s` | min(`time` * (264 / 60), `shots`) | Avg. sustained 264RPM. Somewhat arbitrary |
> | `g8h` | `g8s` | Cannot hit more than shots fired. Ignores bullet penetration intentionally. |
> | `g8k` | `kills` | Cannot get more kills than `kills` |
> | `g3s` | min(`time` * (450 / 60), `shots`) | Avg. sustained 450RPM. Somewhat arbitrary |
> | `g3h` | `g3s` | Cannot hit more than shots fired. Ignores bullet penetration intentionally. |
> | `g3k` | `kills` | Cannot get more kills than `kills` |
> | `g6s` | min(`time` * (450 / 60), `shots`) | --- |
> | `g6h` | `g6s` | --- |
> | `g6k` | `kills` | --- |
> | **Notice:** |  | For the rest of the gun stats, follow the current pattern. The RPM limit will be provided instead of the formula from here on |
> | `g0s` | 400RPM | --- |
> | `g0h` | --- | --- |
> | `g0k` | --- | --- |
> | `g12s` | 550RPM | --- |
> | `g12h` | --- | --- |
> | `g12k` | --- | --- |
> | `g17s` | 600RPM | --- |
> | `g17h` | --- | --- |
> | `g17k` | --- | --- |
> | `g18s` | 30RPM | --- |
> | `g18h` | --- | --- |
> | `g18k` | --- | --- |
> | `g11s` | 600RPM | --- |
> | `g11h` | --- | --- |
> | `g11k` | --- | --- |
> | `g19s` | 200RPM | --- |
> | `g19h` | --- | --- |
> | `g19k` | --- | --- |
> | `g10s` | 600RPM | --- |
> | `g10h` | --- | --- |
> | `g10k` | --- | --- |
> | `g31s` | 700RPM | --- |
> | `g31h` | --- | --- |
> | `g31k` | --- | --- |
> | `g14s` | 600RPM | --- |
> | `g14h` | --- | --- |
> | `g14k` | --- | --- |
> | `g21s` | 750RPM | --- |
> | `g21h` | --- | --- |
> | `g21k` | --- | --- |
> | `g25s` | 750RPM | --- |
> | `g25h` | --- | --- |
> | `g25k` | --- | --- |
> | `g24s` | 600RPM | --- |
> | `g24h` | --- | --- |
> | `g24k` | --- | --- |
> | `g28s` | 100RPM | --- |
> | `g28h` | --- | --- |
> | `g28k` | --- | --- |
> | `g26s` | 700RPM | --- |
> | `g26h` | --- | --- |
> | `g26k` | --- | --- |
> | `g30s` | 50RPM | --- |
> | `g30h` | --- | --- |
> | `g30k` | --- | --- |
> | `g32s` | 700RPM | --- |
> | `g32h` | --- | --- |
> | `g32k` | --- | --- |
> | `g29s` | 50RPM | --- |
> | `g29h` | --- | --- |
> | `g29k` | --- | --- |
> | `g5s` | 450RPM | --- |
> | `g5h` | --- | --- |
> | `g5k` | --- | --- |
> | `g2s` | 300RPM | --- |
> | `g2h` | --- | --- |
> | `g2k` | --- | --- |
> | `g23s` | 700RPM | --- |
> | `g23h` | --- | --- |
> | `g23k` | --- | --- |
> | `g22s` | 750RPM | --- |
> | `g22h` | --- | --- |
> | `g22k` | --- | --- |
> | `g27s` | 50RPM | --- |
> | `g27h` | --- | --- |
> | `g27k` | --- | --- |
> | `g4s` | 350RPM | --- |
> | `g4h` | --- | --- |
> | `g4k` | --- | --- |
> | `g20s` | 200RPM | --- |
> | `g20h` | --- | --- |
> | `g20k` | --- | --- |
> | `g16s` | 600RPM | --- |
> | `g16h` | --- | --- |
> | `g16k` | --- | --- |
> | `g15s` | 40RPM | --- |
> | `g15h` | --- | --- |
> | `g15k` | --- | --- |
> | `g35s` | 2RPM | --- |
> | `g35k` | min(`g35s`, `shots`) | You cannot have more Zeus kills than shots |

After some testing, I've determined that these filters need to be applied per account and then added on. We can't keep a running total and apply them later. A simple thought experiment proves this. Player A gets 100 kills and no backfires. Player B gets 0 kills but 100 backfires. Clearly that total cannot be used since it is obviously not correct. By using a running total, we'll only see 100 kills and 100 backfires, which is perfectly valid, although it actually isn't. Because of this we must apply all filters before adding it to our Mega stats.


## Example Current

```javascript
// Not every single stat is shown because I don't want to manually type it all.
{
    "now":{
        "updated":"2018-02-21T00:23:47Z",
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
        "updated":"2018-02-21T00:23:47Z",
        // ...
    }
}
```

## Example Grand

```javascript
// Not every single stat is shown because I don't want to manually type it all.
{
    "updated":"2018-02-21T00:23:47Z",
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

```javascript
// Not every single stat is shown because I don't want to manually type it all.
{
    "daily":[
        {
            // Imitates playing 4 Deathmatch games.
            "date":"2018-02-21",
            "time":1897,
            "kill":53,
            "deaths":20,
            "damage":7513,
            "rounds":4,
            "rwins":3,
            "knife":1,
            "doms":2,
            "overkill":1,
            "shots":881,
            // ...
        },
        {
            "date":"2018-02-17", // Last played 4 days ago
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

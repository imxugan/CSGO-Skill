    /*********************************************************
     *    This file is licensed under the MIT 2.0 license    *
     *             Last updated March 21st, 2018             *
     *   *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *   *
     *    Please check out the full repository located at    *
     *   http://github.com/almic/CSGO-Skill for some other   *
     * important things like the User Agreement and Privacy  *
     *  Policy, as well as some helpful information on how   *
     *     you can contribute directly to the project :)     *
     *********************************************************/

const request = require('sync-request'),
      crypto = require('crypto'),
      template = require('./template.js')

exports = module.exports

if (process.env.NODE_ENV === 'production') {
    console.log('Production Environment')
} else {
    console.log('Development Environment')
    require('dotenv').config()
}
const MONGOURL = process.env.MONGOURL
const STEAMKEY = process.env.STEAMKEY
const MAILKEY  = process.env.MAILKEY
exports.MONGOURL = MONGOURL
exports.STEAMKEY = STEAMKEY
exports.MAILKEY  = MAILKEY

exports.request = request
exports.template = template

exports.e = function(id, error) {
    if (error) {console.error(`Error occured [${id}]: ${error}`); return true}
    return false
}

exports.consoleResponse = function(output) {
    output = JSON.stringify(output)
    return '<!DOCTYPE html><html><body><script>' +
           `console.log(\`SKILL-BOT:${output}\`)` +
           `</script><span hidden id="out">${output}</span>` +
           'Greetings, human!</body></html>'
}

exports.downloadOwnsCSGO = function(steamid) {
    try {
        var res = request('GET',
                          'http://api.steampowered.com/IPlayerService/GetOwnedGames/' +
                          `v0001/?key=${STEAMKEY}&input_json={%22steamid%22:${steamid},` +
                          '%22appids_filter%22:[730]}')
        res = JSON.parse(res.getBody('utf8'))
        return { 'success': true, 'res': res }
    } catch (e) {
        return { 'success': false, 'error': e }
    }
}

exports.downloadSummaries = function(steamid) {
    try {
        var res = request('GET',
                          'http://api.steampowered.com/ISteamUser/GetPlayerSummaries/' +
                          `v0002/?key=${STEAMKEY}&steamids=${steamid}`)
        res = JSON.parse(res.getBody('utf8'))
        return { 'success': true, 'res': res }
    } catch (e) {
        return { 'success': false, 'error': e }
    }
}

exports.downloadStats = function(steamid) {
    try {
        var res = request('GET',
                          'http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/' +
                          `v2/?appid=730&key=${STEAMKEY}&steamid=${steamid}`)
        res = convertApi(JSON.parse(res.getBody('utf8')))
        return { 'success': true, 'res': res }
    } catch (e) {
        return { 'success': false, 'error': e }
    }

}

// Returns a random number between positive min and max
exports.randNum = function(min, max) {
    if (!Number.isSafeInteger(min) || !Number.isSafeInteger(max)) {
        throw new Error('Both min and max must be numbers between -(2^53 - 1) and (2^53 - 1)')
    }
    if (min > max) {
        const range = min - max
    } else if (max > min){
        const range = max - min
    } else {
        throw new Error('One parameter must be larger than the other')
    }
    if (range >= Math.pow(2, 32)) {
        // This doesn't work right with ranges over 2^32
        throw new Error('The range is too large! Must be less than 2^32.')
    }

    var tmp = range
    var bitsNeeded = 0
    var bytesNeeded = 0
    var mask = 1

    while (tmp > 0) {
        if (bitsNeeded % 8 === 0) bytesNeeded += 1
        bitsNeeded += 1
        mask = mask << 1 | 1
        tmp = tmp >>> 1
    }
    const randomBytes = crypto.randomBytes(bytesNeeded)
    var randomValue = 0

    for (var i = 0; i < bytesNeeded; i++) {
        randomValue |= randomBytes[i] << 8 * i
    }

    randomValue = randomValue & mask;

    if (randomValue <= range) {
        return min + randomValue
    } else {
        return randNum(min, max)
    }
}

// Generates a random sha256 hash token. Should only be used temporarily and regularly changed
exports.getToken = function() {
    return {
        'time': +new Date(),
        'value': crypto.createHash('sha256').update(crypto.randomBytes(128)).digest('hex')
    }
}

// Global definition for a valid token
exports.isValidToken = function(token, limit) {
    if (typeof token === 'object' &&
        typeof token.value === 'string' &&
        token.value.length === 64 &&
        typeof token.time === 'number') {

        var reg = /^[0-9a-fA-F]+$/
        if (!reg.test(token.value)) { return false }
        var seconds = Math.abs(token.time - (+new Date())) / 1000
        if (!limit) { limit = 6 * 24 * 60 * 60 } // default 6 day limit before resetting tokens
        return seconds < limit
    }
    return false
}

/**
 * Given a steamid, this function get's the player summaries and returns an object with `success`
 * true and some other data points if the account meets all of the following requirements. Otherwise
 * it returns a list of the requirements it does not meet.
 *
 * - Public profile
 * - Owns CS: GO
 * - Has 10 hours played (600 minutes)
 *
 * Object for successful validation has all of these:
 *
 * - persona
 * - profileurl
 * - avatarurl
 * - steamid
 */
exports.validateAccount = function(steamid) {
    var summary = exports.downloadSummaries(steamid)
    if (!summary.success) {
        assistant.e(206, summary.error)
        return { 'success': false, 'reason': 'error code 206' }
    }

    if (summary.res.response.players[0].steamid !== steamid) {
        return { 'success': false, 'reason': 'failed to validate account' }
    }

    var player = summary.res.response.players[0]

    if (player.communityvisibilitystate !== 3) {
        return { 'success': false, 'reason': 'private' }
    }

    var games = exports.downloadOwnsCSGO(steamid)
    if (!games.success) {
        assistant.e(207, games.error)
        return { 'success': false, 'reason': 'error code 207' }
    }

    if (games.res.response.game_count !== 1) {
        return { 'success': false, 'reason': 'own' }
    }

    if (games.res.response.games[0].playtime_forever < 600) {
        return { 'success': false, 'reason': 'playtime' }
    }

    // Good to go!

    // Skip http://steamcommunity.com/
    player.profileurl = player.profileurl.substr(player.profileurl.indexOf('.com/') + 5)
    // Skip https://steamcdn-a.akamaihd.net/steamcommunity/public/images/avatars/ and the .jpg
    player.avatar = player.avatar.substring(player.avatar.indexOf('/avatars/') + 9, player.avatar.length - 4)
    return {
        'success': true,
        'profile': {
            'steam_id': steamid,
            'token': exports.getToken(),
            'persona': player.personaname,
            'profileurl': player.profileurl,
            'avatarurl': player.avatar,
            'created': +new Date(),
            'status': {'level':'player'}
        }
    }

}

// This is a utility function to make sure Steam IDs are correct Steam IDs.
exports.isSteamId = function(number) {
    try {
        var num = number.toString()
        if (num.length < 16 || num.length > 18) {
            // HIGHLY unlikely to be an account. All Steam IDs seem to be 17 digits
            return false
        }
        var s = '0123456789'
        for (var i = 0; i < num.length; i++) {
            if (s.indexOf(num[i]) < 0) {
                return false
            }
        }
        return true
    } catch (e) {
        return false
    }
}

exports.isEmail = function(email) {
    try {
        var em = email.toString()
        if (em.length < 4 || em.length > 50) {
            return false
        }
        return (em.indexOf('@') > 0) // If '@' is the first character, assume it's not real
    } catch (e) {
        return false
    }
}

exports.fixUsername = function(username) {
    try {
        var name = username.toString().toLowerCase()
        if (name.length < 3) {
            return { 'success': false }
        }
        var banned = '.~ :/\\?&!#[]@$\'"()*+,;=%^{}|`<>'
        var fname = ''
        for (var i = 0; i < name.length; i++) {
            if (banned.indexOf(name[i]) < 0) { fname += name[i] }
        }
        if (fname.length < 3 || fname.length > 35) {
            return { 'success': false }
        }
        return { 'success': true, 'username': fname }
    } catch (e) {
        return { 'success': false }
    }
}

exports.getStatTemplate = function(steamid, isUser) {
    if (typeof isUser !== 'boolean') { isUser = false }
    if (isUser) {
        return [
            'knife',         'nades',         'fires',         'doms',
            'revs',          'overkill',      'pistolwin',     'donation',
            'wins_cbble',    'wins_dust2',    'wins_inferno',  'wins_nuke',
            'wins_train',    'rnds_cbble',    'rnds_dust2',    'rnds_inferno',
            'rnds_nuke',     'rnds_train'
        ]
    }
    var current = exports.downloadStats(steamid)
    if (current.success) {
        current.res.updated = +new Date()
        current = {
            'now': current.res,
            'recent': current.res
        }
    } else {
        exports.e(400, new Error(`Failed to retrieve stats for ${steamid}. "${current.error}"`))
        current = {}
    }
    var template = {
        'steam_id': steamid,
        'user': false,
        'visits': [],
        'active': +new Date(),
        'current': current,
        'grand': {
            'time': 0,      'kills': 0,     'deaths': 0,    'heads': 0,     'plants': 0,
            'defuse': 0,    'hostage': 0,   'rounds': 0,    'rwins': 0,     'matches': 0,
            'mwins': 0,     'shots': 0,     'hits': 0,      'damage': 0,    'money': 0,
            'contrib': 0,   'mvps': 0
        },
        'history': {
            'daily': [],
            'monthly': [],
        }
    }
    return template
}

function convertApi(stats) {
    var c = {}
    var r = {}
    stats = stats.playerstats.stats
    for (var i = 0; i < stats.length; i++) {
        c[stats[i].name] = stats[i].value
    }
    r.kills     = c.total_kills
    r.deaths    = c.total_deaths
    r.time      = c.total_time_played
    r.heads     = c.total_kills_headshot
    r.rwins     = c.total_wins
    r.rounds    = c.total_rounds_played
    r.mwins     = c.total_matches_won
    r.matches   = c.total_matches_played
    r.shots     = c.total_shots_fired
    r.hits      = c.total_shots_hit
    r.damage    = c.total_damage_done
    r.plants    = c.total_planted_bombs
    r.defuse    = c.total_defused_bombs
    r.hostage   = c.total_rescued_hostages
    r.contrib   = c.total_contribution_score
    r.money     = c.total_money_earned
    r.mvps      = c.total_mvps
    r.knife     = c.total_kills_knife
    r.nades     = c.total_kills_hegrenade
    r.fires     = c.total_kills_molotov
    r.csnipe    = c.total_kills_against_zoomed_sniper
    r.backfire  = c.total_kills_enemy_weapon
    r.doms      = c.total_dominations
    r.revs      = c.total_revenges
    r.overkill  = c.total_domination_overkills
    r.pistolwin = c.total_wins_pistolround
    r.donation  = c.total_weapons_donated

    r.wins_cbble   = c.total_wins_map_de_cbble
    r.wins_dust2   = c.total_wins_map_de_dust2
    r.wins_inferno = c.total_wins_map_de_inferno
    r.wins_nuke    = c.total_wins_map_de_nuke
    r.wins_train   = c.total_wins_map_de_train

    r.rnds_cbble   = c.total_rounds_map_de_cbble
    r.rnds_dust2   = c.total_rounds_map_de_dust2
    r.rnds_inferno = c.total_rounds_map_de_inferno
    r.rnds_nuke    = c.total_rounds_map_de_nuke
    r.rnds_train   = c.total_rounds_map_de_train

//  Glock-18                                     P2000 & USP-S
    r.g0s  = c.total_shots_glock;                r.g2s  = c.total_shots_hkp2000
    r.g0h  = c.total_hits_glock;                 r.g2h  = c.total_hits_hkp2000
    r.g0k  = c.total_kills_glock;                r.g2k  = c.total_kills_hkp2000
//  Dual Berettas                                P250
    r.g3s  = c.total_shots_elite;                r.g4s  = c.total_shots_p250
    r.g3h  = c.total_hits_elite;                 r.g4h  = c.total_hits_p250
    r.g3k  = c.total_kills_elite;                r.g4k  = c.total_kills_p250
//  Tec-9 & CZ75-Auto                            Five Seven & CZ75-Auto
    r.g5s  = c.total_shots_tec9;                 r.g6s  = c.total_shots_fiveseven
    r.g5h  = c.total_hits_tec9;                  r.g6h  = c.total_hits_fiveseven
    r.g5k  = c.total_kills_tec9;                 r.g6k  = c.total_kills_fiveseven
//  Desert Eagle & R8 Revolver                   Galil AR
    r.g8s  = c.total_shots_deagle;               r.g10s = c.total_shots_galilar
    r.g8h  = c.total_hits_deagle;                r.g10h = c.total_hits_galilar
    r.g8k  = c.total_kills_deagle;               r.g10k = c.total_kills_galilar
//  FAMAS                                        AK-47
    r.g11s = c.total_shots_famas;                r.g12s = c.total_shots_ak47
    r.g11h = c.total_hits_famas;                 r.g12h = c.total_hits_ak47
    r.g11k = c.total_kills_famas;                r.g12k = c.total_kills_ak47
//  M4A1-S & M4A4                                SSG 08
    r.g14s = c.total_shots_m4a1;                 r.g15s = c.total_shots_ssg08
    r.g14h = c.total_hits_m4a1;                  r.g15h = c.total_hits_ssg08
    r.g14k = c.total_kills_m4a1;                 r.g15k = c.total_kills_ssg08
//  SG 553                                       AUG
    r.g16s = c.total_shots_sg556;                r.g17s = c.total_shots_aug
    r.g16h = c.total_hits_sg556;                 r.g17h = c.total_hits_aug
    r.g16k = c.total_kills_sg556;                r.g17k = c.total_kills_aug
//  AWP                                          G3SG1
    r.g18s = c.total_shots_awp;                  r.g19s = c.total_shots_g3sg1
    r.g18h = c.total_hits_awp;                   r.g19h = c.total_hits_g3sg1
    r.g18k = c.total_kills_awp;                  r.g19k = c.total_kills_g3sg1
//  SCAR-20                                      MAC-10
    r.g20s = c.total_shots_scar20;               r.g21s = c.total_shots_mac10
    r.g20h = c.total_hits_scar20;                r.g21h = c.total_hits_mac10
    r.g20k = c.total_kills_scar20;               r.g21k = c.total_kills_mac10
//  MP9                                          MP7
    r.g22s = c.total_shots_mp9;                  r.g23s = c.total_shots_mp7
    r.g22h = c.total_hits_mp9;                   r.g23h = c.total_hits_mp7
    r.g22k = c.total_kills_mp9;                  r.g23k = c.total_kills_mp7
//  UMP-45                                       P90
    r.g24s = c.total_shots_ump45;                r.g25s = c.total_shots_p90
    r.g24h = c.total_hits_ump45;                 r.g25h = c.total_hits_p90
    r.g24k = c.total_kills_ump45;                r.g25k = c.total_kills_p90
//  PP-Bizon                                     Nova
    r.g26s = c.total_shots_bizon;                r.g27s = c.total_shots_nova
    r.g26h = c.total_hits_bizon;                 r.g27h = c.total_hits_nova
    r.g26k = c.total_kills_bizon;                r.g27k = c.total_kills_nova
//  XM1014                                       Sawed-Off
    r.g28s = c.total_shots_xm1014;               r.g29s = c.total_shots_sawedoff
    r.g28h = c.total_hits_xm1014;                r.g29h = c.total_hits_sawedoff
    r.g28k = c.total_kills_xm1014;               r.g29k = c.total_kills_sawedoff
//  MAG-7                                        M249
    r.g30s = c.total_shots_mag7;                 r.g31s = c.total_shots_m249
    r.g30h = c.total_hits_mag7;                  r.g31h = c.total_hits_m249
    r.g30k = c.total_kills_mag7;                 r.g31k = c.total_kills_m249
//  Negev                                        Zeus x27
    r.g32s = c.total_shots_negev;                r.g35s = c.total_shots_taser
    r.g32h = c.total_hits_negev;                 r.g35k = c.total_kills_taser
    r.g32k = c.total_kills_negev

    return r

}

class Timer {
    constructor(label) {
        this.label = label + ': '
        this.start = +new Date()
    }

    stop() {
        if (this.start === null) {
            console.error('Cannot access a timer that has called stop()')
            return;
        }
        console.log(this.label + ((+new Date()) - this.start) + 'ms')
        this.label = null
        this.start = null
    }
}

exports.timer = function (label = 'Time Taken') {
    return new Timer(label)
}

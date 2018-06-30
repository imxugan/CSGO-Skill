    /*********************************************************
     *    This file is licensed under the MIT 2.0 license    *
     *              Last updated June 6th, 2018              *
     *   *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *   *
     *    Please check out the full repository located at    *
     *   http://github.com/almic/CSGO-Skill for some other   *
     * important things like the User Agreement and Privacy  *
     *  Policy, as well as some helpful information on how   *
     *     you can contribute directly to the project :)     *
     *********************************************************/

console.log('Starting Worker')
var start = Number(new Date())

const FeedParser = require('feedparser')
    , BigNumber = require('big-number')
    , request = require('request')
    , requestSync = require('sync-request') // Specifically for stats tracking, so work is atomic
    , mailer = require('@sendgrid/mail')
    , ejs = require('ejs')

const template = require('./template.js')
    , skill = require('./mongo.js')

if (process.env.NODE_ENV !== 'production')
    require('dotenv').config()


const REDIS_URL = process.env.REDIS_URL
const MONGOURL  = process.env.MONGOURL
const STEAMKEY  = process.env.STEAMKEY
const MAILKEY   = process.env.MAILKEY

const STATS_LINK = `http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v2/?appid=730&key=${STEAMKEY}&steamid=`

var queue = require('nque').createQueue(REDIS_URL)
skill.connect(MONGOURL)
skill.db('collection:Single', (err, db, col) => {
    if (err) throw err

    col.findOne({name:'test'}, (err, doc) => {
        if (err) throw err

        db.close()
        if (doc.test === 'abc123')
            console.log('Successfully connected to MongoDB')

    })
})
mailer.setApiKey(MAILKEY)

function sendMail(opts) {
    var sub = opts.sub
    var subject = opts.subject
    var content = opts.content
    var from = 'Skill Bot <skillbot@csgo-skill.com>'
    skill.db('collection:Players', (err, db, col) => {
        if (err) {
            db.close()
            return console.error(`sendMail to '${sub}' failed with content: ${content.substr(0,50)}`)
        }
        var ob = {}
        ob['subscription.' + sub] = true
        col.find(ob).toArray((err, docs) => {
            db.close()

            if (err)
                return console.error(`sendMail to '${sub}' failed on find(): ${err}`)

            var to = []
            docs.forEach(doc => {
                to.push(doc.email)
            })

            if (to.length > 0)
                mailer.sendMultiple({
                    to: to,
                    from: from,
                    subject: subject,
                    html: content
                }).then(() => {
                    mailer.send({
                        to: 'devteam@csgo-skill.com',
                        from: 'Skill Bot <skillbot@csgo-skill.com>',
                        subject: `${to.length} Emails Sent To '${sub}'`,
                        html: `I just emailed <b>${to.length}</b> addresses subscribed to the <b>${sub}</b> mail list.`
                    }).then(() => { /* Nothing to do here */ })
                    .catch(error => {
                        console.error(`sendMail to devteam failed: ${error}`)
                    })
                }).catch(error => {
                    console.error(`sendMail to '${sub}' failed on sendMultiple(): ${error}`)
                })
        })
    })
}

function convertApi(steamId, stats) {
    var c = {}
    var r = {}
    try {
        stats = stats.playerstats.stats

        for (var i = 0; i < stats.length; i++)
            c[stats[i].name] = stats[i].value

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
        r.flash     = c.total_kills_enemy_blinded
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

        for (let prop in r)
            if (r.hasOwnProperty(prop) && !r[prop]) r[prop] = 0

        return {error: null, result: r}

    } catch (err) {
        console.error(`Error while converting stats for steamId '${steamId}': ${err}`)
        return {error: err, result: r} // No matter what, return whatever we got.
    }

}

queue.processJob('check-updates', (job, done) => {
    console.log(`Processing Job ${job.id}: 'check-updates'`)
    var startTime = Number(new Date())
    var req = request('http://blog.counter-strike.net/index.php/category/updates/feed/')
    var parser = new FeedParser()
    var feed = []

    req.on('error', error => {
        console.error(`Request error for 'check-updates': ${error}`)
        done(error)
    })

    req.on('response', res => {
        if (res.statusCode !== 200)
            req.emit('error', new Error(`Bad status code: ${res.statusCode}`))
        else
            req.pipe(parser)
    })

    parser.on('error', error => {
        console.error(`Parser error for 'check-updates': ${error}`)
        done(error)
    })

    parser.on('readable', () => {
        try {
            var item = parser.read()
            if (item !== null)
                feed.push({
                    title: item.title,
                    link: item.link,
                    published: new Date(item.pubDate),
                    discovered: new Date(),
                    content: item.description
                })

        } catch (e) {
            parser.emit('error', e)
        }
    })

    parser.on('end', () => {
        try {
            console.log(`Parsed ${feed.length} update articles in ${Number(new Date()) - startTime}ms`)
            var self = parser
            skill.db('collection:CSGOUpdates', (err, db, col) => {
                if (err) {
                    db.close()
                    return self.emit('error', err)
                }
                var check = []
                feed.forEach(item => {
                    check.push({link: item.link})
                })
                check = { $or: check }
                col.find(check).toArray((err, docs) => {
                    if (err)
                        return self.emit('error', err)

                    var item, found, i, j
                    var toAdd = []

                    for (i = 0; i < feed.length; i++) {
                        found = false
                        item = feed[i]
                        for (j = 0; j < docs.length; j++)
                            if (item.link === docs[j].link) {
                                found = true
                                break
                            }

                        if (!found) toAdd.push(item)
                    }

                    if (toAdd.length > 0) {
                        console.log(`Found ${toAdd.length} new article${toAdd.length > 1 ? 's' : ''}!`)
                        col.insertMany(toAdd, {}, (err, result) => {
                            db.close()
                            if (err)
                                return self.emit('error', err)

                            sendMail({
                                sub: 'csgoupdates',
                                subject: `CSGO Update: ${result.ops[0].title}`,
                                content: ejs.render(template.email.csgoupdate, {'items': result.ops})
                            })
                        })
                    } else { db.close() }
                    console.log('Job \'check-updates\' completed successfully')
                    done()
                })
            })
        } catch (e) {
            parser.emit('error', e)
        }
    })

})

/* TODO:
 * The single account updates will be included when the website is running at full force. It should
 * integrate with the support ticket system so that if a retry for a single account fails, it will
 * just open a new ticket for the account which the devs can go through later.
 *
 * The current plan is that if less than 50 accounts fail, then each gets a retry link sent to their
 * email if they have an account, otherwise the account is skipped entirely. That number will likely
 * increase as more players sign up, but should always be less than 20% of total players.
 *
 * A good idea for the retry link is to show a page with multiple instructions about what they need
 * to do before running a retry. Included should be making sure the profile is totally public, and
 * a special instruction. A checkbox saying "I've read and done these steps, I'm ready to start" will
 * be above the button, which is initially disabled. The special instruction, however, will request
 * the user to NOT check this box, and instead wait 15 seconds for the button to become enabled, and
 * click it without checking the box.
 *
 * Obviously if the user checks the box, they did not properly read the instructions nor did they
 * log into Steam to make sure their account was public. It should take at least 15 seconds to go
 * through the list of steps before clicking the button. If they check the box, and click the button,
 * it should just reload the page and display an error at the top saying "You did NOT correctly
 * follow the instructions... please try again."
 *
 * As for the "less than 50 accounts fail," that is to prevent a global stat failure from sending
 * links to every player, ultimately resulting in tons of support tickets in the event that the
 * Steam servers were down during the stat update. Such an event in which more than 50% of all
 * accounts fail, should email the devs directly telling them to manually re-run the stat update
 * once Steam is back up, or to correct whatever bug caused most accounts to fail.
 */
// This is for single accounts, and may be run only once per user
queue.processJob('update-stats-single', (job, done) => {

    var steamId = job.data.steamId

    skill.db('collection:Players', (error, db, col) => {
        if (error) {
            db.close()
            // There's not much we can do here
            return
        }
        col.findOne({'steam_id': steamId}).toArray((err, doc) => {
            if (err) {
                db.close()
                // Still nothing we can do
                return
            }
            if (doc.steam_id !== steamId) {
                db.close()
                // Redundancy check if we found an account
                return
            }

            var player = doc
            var timeLimit = Number(new Date()) - (20 * 60 * 60 * 1000) // 20 hours to retry daily

            if (player.status.updateFailed.status === 'waiting' && player.status.updateFailed.time > timeLimit)
                col.findOneAndUpdate({steam_id: player.steam_id}, {
                    $set: {
                        'status.updateFailed.status': 'working',
                        'status.updateFailed.jobid': job.id,
                        'status.updateFailed.timeStarted': Number(new Date())
                    }
                }, (err, result) => {
                    if (err) {
                        // TODO
                    }
                    // TODO
                })

        })
    })
})

// This is for all accounts, so no setup is necessary
queue.processJob('update-stats', (job, done) => {
    updateStats('all', job, done)
})

function updateStats(type, job, done) {
    if (type === 'all')
        console.log(`Processing Job ${job.id}: 'update-stats'`)
    else if (type === 'single')
        console.log(`Processing Job ${job.id}: 'update-stats-single' for user ${job.data.steamId}`)

    /* This function will attempt to recreate the job up to 10 times in the event of complete
     * failures to do any stat updates. When all attempts have been exausted, it emails the dev team
     * asking for manual assistance to correct whatever error is happening.
     */
    var noConnection = function (error, reason) {
        if (!job.data.attempts)
            job.data.attempts = 1
        else
            job.data.attempts++

        console.error(`Job ${job.id}: 'update-stats' failed (${job.data.attempts}) ${reason}: ${error}`)
        if (job.data.attempts > 10)
            mailer.send({
                to: 'devteam@csgo-skill.com',
                from: 'Skill Bot <skillbot@csgo-skill.com>',
                subject: 'Job \'update-stats\' has failed',
                html: 'After trying 10 times, I failed to complete the \'update-stats\' job. Please help!'
            }).then(() => { /* Nothing to do here */ })
            .catch(error => { console.error(`Job ${job.id}: 'update-stats' sendMail to devteam failed: ${error}`) })
        else
            setTimeout(queue.createJob('update-stats', job.data).run, 5000)

        done()
    }

    // Initialize vars
    var timeStarted = Number(new Date()),
        req, i, player, stats, stat,
        changes, changed

    var playerInfo = {
        total: 0,
        active: 0,
        checked: 0,
        skipped: 0,
        changes: 0,
        noChanges: 0,
        updated: 0,
        updatedNoChanges: 0,
        failedRequests: [],
        badRequests: [],
        badStats: [],
        badUpdate: []
    }

    var grand = [
        'time', 'kills', 'deaths', 'heads', 'plants', 'defuse', 'hostage', 'rounds',
        'rwins', 'matches', 'mwins', 'shots', 'hits', 'damage', 'money', 'contrib', 'mvps'
    ]
    var grandUser = grand.concat([
        'knife', 'nades', 'fires', 'flash', 'doms', 'revs', 'overkill', 'pistolwin',
        'donation', 'wins_cbble', 'wins_dust2', 'wins_inferno', 'wins_nuke',
        'wins_train', 'rnds_cbble', 'rnds_dust2', 'rnds_inferno', 'rnds_nuke',
        'rnds_train'
    ])

    var mega = {
        kills:     new BigNumber(),
        deaths:    new BigNumber(),
        time:      new BigNumber(),
        heads:     new BigNumber(),
        rwins:     new BigNumber(),
        rounds:    new BigNumber(),
        mwins:     new BigNumber(),
        matches:   new BigNumber(),
        shots:     new BigNumber(),
        hits:      new BigNumber(),
        damage:    new BigNumber(),
        plants:    new BigNumber(),
        defuse:    new BigNumber(),
        hostage:   new BigNumber(),
        contrib:   new BigNumber(),
        money:     new BigNumber(),
        mvps:      new BigNumber(),
        knife:     new BigNumber(),
        nades:     new BigNumber(),
        fires:     new BigNumber(),
        flash:     new BigNumber(),
        csnipe:    new BigNumber(),
        backfire:  new BigNumber(),
        doms:      new BigNumber(),
        revs:      new BigNumber(),
        overkill:  new BigNumber(),
        pistolwin: new BigNumber(),
        donation:  new BigNumber(),

        wins_cbble:   new BigNumber(),
        wins_dust2:   new BigNumber(),
        wins_inferno: new BigNumber(),
        wins_nuke:    new BigNumber(),
        wins_train:   new BigNumber(),

        rnds_cbble:   new BigNumber(),
        rnds_dust2:   new BigNumber(),
        rnds_inferno: new BigNumber(),
        rnds_nuke:    new BigNumber(),
        rnds_train:   new BigNumber(),

    //  Glock-18                                  P2000 & USP-S
        g0s: new BigNumber(),                     g2s: new BigNumber(),
        g0h: new BigNumber(),                     g2h: new BigNumber(),
        g0k: new BigNumber(),                     g2k: new BigNumber(),
    //  Dual Berettas                             P250
        g3s: new BigNumber(),                     g4s: new BigNumber(),
        g3h: new BigNumber(),                     g4h: new BigNumber(),
        g3k: new BigNumber(),                     g4k: new BigNumber(),
    //  Tec-9 & CZ75-Auto                         Five Seven & CZ75-Auto
        g5s: new BigNumber(),                     g6s: new BigNumber(),
        g5h: new BigNumber(),                     g6h: new BigNumber(),
        g5k: new BigNumber(),                     g6k: new BigNumber(),
    //  Desert Eagle & R8 Revolver                Galil AR
        g8s: new BigNumber(),                     g10s: new BigNumber(),
        g8h: new BigNumber(),                     g10h: new BigNumber(),
        g8k: new BigNumber(),                     g10k: new BigNumber(),
    //  FAMAS                                     AK-47
        g11s: new BigNumber(),                    g12s: new BigNumber(),
        g11h: new BigNumber(),                    g12h: new BigNumber(),
        g11k: new BigNumber(),                    g12k: new BigNumber(),
    //  M4A1-S & M4A4                             SSG 08
        g14s: new BigNumber(),                    g15s: new BigNumber(),
        g14h: new BigNumber(),                    g15h: new BigNumber(),
        g14k: new BigNumber(),                    g15k: new BigNumber(),
    //  SG 553                                    AUG
        g16s: new BigNumber(),                    g17s: new BigNumber(),
        g16h: new BigNumber(),                    g17h: new BigNumber(),
        g16k: new BigNumber(),                    g17k: new BigNumber(),
    //  AWP                                       G3SG1
        g18s: new BigNumber(),                    g19s: new BigNumber(),
        g18h: new BigNumber(),                    g19h: new BigNumber(),
        g18k: new BigNumber(),                    g19k: new BigNumber(),
    //  SCAR-20                                   MAC-10
        g20s: new BigNumber(),                    g21s: new BigNumber(),
        g20h: new BigNumber(),                    g21h: new BigNumber(),
        g20k: new BigNumber(),                    g21k: new BigNumber(),
    //  MP9                                       MP7
        g22s: new BigNumber(),                    g23s: new BigNumber(),
        g22h: new BigNumber(),                    g23h: new BigNumber(),
        g22k: new BigNumber(),                    g23k: new BigNumber(),
    //  UMP-45                                    P90
        g24s: new BigNumber(),                    g25s: new BigNumber(),
        g24h: new BigNumber(),                    g25h: new BigNumber(),
        g24k: new BigNumber(),                    g25k: new BigNumber(),
    //  PP-Bizon                                  Nova
        g26s: new BigNumber(),                    g27s: new BigNumber(),
        g26h: new BigNumber(),                    g27h: new BigNumber(),
        g26k: new BigNumber(),                    g27k: new BigNumber(),
    //  XM1014                                    Sawed-Off
        g28s: new BigNumber(),                    g29s: new BigNumber(),
        g28h: new BigNumber(),                    g29h: new BigNumber(),
        g28k: new BigNumber(),                    g29k: new BigNumber(),
    //  MAG-7                                     M249
        g30s: new BigNumber(),                    g31s: new BigNumber(),
        g30h: new BigNumber(),                    g31h: new BigNumber(),
        g30k: new BigNumber(),                    g31k: new BigNumber(),
    //  Negev                                     Zeus x27
        g32s: new BigNumber(),                    g35s: new BigNumber(),
        g32h: new BigNumber(),                    g35k: new BigNumber(),
        g32k: new BigNumber()
    }

    skill.db('collection:Stats', (error, db, col) => {
        if (error) {
            db.close()
            return noConnection(error, 'connecting to MongoDB')
        }
        var search
        if (type === 'all')
            search = {}
        else if (type === 'single')
            search = {steam_id: job.data.steamId}
        col.find(search).toArray((err, docs) => {
            if (err) {
                db.close()
                if (type === 'all')
                    noConnection(error, 'getting profiles from Stats')
                return
            }
            playerInfo.total = docs.length - 1 // Don't include MEGA
            var today = new Date()
            today = new Date(today.getFullYear(), today.getMonth(), today.getDate())
            console.log(`Job ${job.id}: 'update-stats' Processing ${playerInfo.total} accounts for ${today.toUTCString()}`)
            var timePlayersStarted = Number(new Date())

            var activeLimit = Number(new Date()) - (30 * 24 * 60 * 60 * 1000),  // 30 days
                visitLimit = Number(new Date()) - (7 * 24 * 60 * 60 * 1000),    //  7 days
                trackLimit = Number(new Date()) - (48 * 60 * 60 * 1000)         // 48 hours

            for (i = 0; i < (playerInfo.total + 1); i++) { // Add MEGA back to total
                player = docs[i]

                // Skip MEGA
                if (player.steam_id === 'mega') continue

                // Not active in last 30 days
                if (player.active < activeLimit) {
                    // Most recent visit should be within last week
                    if (!player.user || player.visits.length < 1 || player.visits[0].time < visitLimit) {
                        playerInfo.skipped++
                        continue
                    }
                } else {
                    playerInfo.active++
                }

                // Ready to track
                playerInfo.checked++
                try {
                    req = requestSync('GET', STATS_LINK + player.steam_id)
                    if (req.statusCode !== 200) {
                        playerInfo.failedRequests.push({
                            steam_id: player.steam_id,
                            response: req
                        })
                        continue
                    }
                } catch (error) {
                    playerInfo.failedRequests.push({
                        steam_id: player.steam_id,
                        error: error
                    })
                    continue
                }

                try {
                    stats = convertApi(player.steam_id, JSON.parse(req.getBody('utf8')))
                } catch (error) {
                    //console.error(`Error while parsing stats for steamId '${player.steam_id}': ${error}`)
                    playerInfo.badRequests.push({
                        steam_id: player.steam_id,
                        error: error,
                        response: req
                    })
                    continue // Only try this once per user
                }

                // Despite a possible error here, we may still have some stats returned, so DO NOT `continue`
                if (stats.error)
                    playerInfo.badStats.push({
                        steam_id: player.steam_id,
                        error: stats.error
                    })

                stats = stats.result
                changes = {}

                var g
                if (player.user)
                    g = grandUser
                else
                    g = grand

                // Update all the stats if tracking
                changed = false

                if (player.current.now.updated > trackLimit)
                    for (stat in stats) {
                        if (!stats.hasOwnProperty(stat))
                            continue

                        // Skip if stats haven't changed
                        if (player.current.now[stat] === stats[stat]) {
                            player.current.now[stat] = stats[stat] // Just in case
                            continue
                        }

                        // If stats are not zero or undefined
                        if (player.current.now[stat]) {
                            changes[stat] = stats[stat] - player.current.now[stat]
                            changed = true
                            if (g.includes(stat) && player.grand[stat])
                                player.grand[stat] += changes[stat]
                            else if (g.includes(stat))
                                player.grand[stat] = changes[stat]
                        } else if (g.includes(stat) && !player.grand[stat]) {
                            player.grand[stat] = 0
                        }

                        player.current.now[stat] = stats[stat]
                    }
                else
                    // Otherwise, just update current stats so daily tracking can resume tomorrow
                    for (stat in stats)
                        if (stats.hasOwnProperty(stat))
                            player.current.now[stat] = stats[stat]

                player.current.now.updated = Number(new Date())
                player.current.recent = player.current.now

                if (changed) {
                    playerInfo.changes++
                } else {
                    playerInfo.noChanges++
                    col.findOneAndUpdate({steam_id: player.steam_id}, {
                        $set: { 'current': player.current }
                    }, (err, _) => {
                        if (err)
                            playerInfo.badUpdate.push({
                                steam_id: player.steam_id,
                                error: err
                            })
                        else
                            playerInfo.updatedNoChanges++
                    })
                    continue
                }

                // History stats
                changes.date = today
                player.history.daily.unshift(changes)
                if (player.user) {
                    // Push +3 month old dailies into monthly entries
                    var monthlyLimit = Number(today) - (90 * 24 * 60 * 60 * 1000) // 90 days

                    // j = 1, because we can definitely skip the first one
                    for (var j = 1; j < player.history.daily.length; j++) {
                        var dailyEntry = player.history.daily[j]
                        if (dailyEntry.date < monthlyLimit) {
                            var found = false
                            var mDate = dailyEntry.date
                            mDate = new Date(mDate.getFullYear(), mDate.getMonth())
                            for (var k = 0; k < player.history.monthly.length; k++) {
                                if (player.history.monthly[k].date !== mDate)
                                    continue

                                found = true
                                for (stat in dailyEntry) {
                                    if (!dailyEntry.hasOwnProperty(stat) || stat === 'date')
                                        continue

                                    if (typeof player.history.monthly[k][stat] === 'number')
                                        player.history.monthly[k][stat] += dailyEntry[stat]
                                    else
                                        player.history.monthly[k][stat] = dailyEntry[stat]
                                }

                                player.history.daily.splice(j, 1)
                                j-- // Fix element skipping after altering indexes with splice()
                                break
                            }

                            // If no monthly exists, set date to Year-Month and add that
                            if (!found) {
                                dailyEntry.date = mDate
                                player.history.monthly.unshift(dailyEntry)
                                player.history.daily.splice(j, 1)
                                j--
                            }
                        }
                    }
                } else {
                    /* Remove last entries if over 30 exist
                     * Technically we say that we save the last 30 days of stats, but that really
                     * means we save the last 30 days of stat *changes*, which means you could have
                     * a stat record older than a month saved on your account.
                     */
                    player.history.daily.splice(30)
                }

                // Update player stats and continue
                col.findOneAndUpdate({steam_id: player.steam_id}, {
                    $set: {
                        'current': player.current,
                        'grand': player.grand,
                        'history': player.history
                    }
                }, (err, _) => {
                    if (err)
                        playerInfo.badUpdate.push({
                            steam_id: player.steam_id,
                            error: err
                        })
                    else
                        playerInfo.updated++
                })

                // And Now For Mega Stats

                var m = function (stat) { return (stat && stat > 0) }
                var vpm = function (value, time, pm) {
                    return Math.min(value, (time * (pm / 60)).toFixed(0))
                }

                // Limits

                // 1 day of round time
                changes.time =
                    m(changes.time) ? Math.min(changes.time, 86400) : 0

                // 10 kills per minute
                changes.kills =
                    m(changes.kills) ? vpm(changes.kills, changes.time, 10) : 0

                // 10 deaths per minute
                changes.deaths =
                    m(changes.deaths) ? vpm(changes.deaths, changes.time, 10) : 0

                // Cannot get more headshot kills than kills
                changes.heads =
                    m(changes.heads) ? Math.min(changes.heads, changes.kills) : 0

                // 1 round per 30 seconds, or 2 per minute
                changes.rounds =
                    m(changes.rounds) ? vpm(changes.rounds, changes.time, 2) : 0

                // 1 match per 30 seconds, or 2 per minute
                changes.matches =
                    m(changes.matches) ? vpm(changes.matches, changes.time, 2) : 0

                // Cannot win more than rounds played
                changes.rwins =
                    m(changes.rwins) ? Math.min(changes.rwins, changes.rounds) : 0

                // Cannot win more than matches played
                changes.mwins =
                    m(changes.mwins) ? Math.min(changes.mwins, changes.matches) : 0

                // Avg. sustained 750RPM, somewhat arbitrary
                changes.shots =
                    m(changes.shots) ? vpm(changes.shots, changes.time, 750) : 0

                // Ignoring bullet penetration, cannot hit more than shots taken
                changes.hits =
                    m(changes.hits) ? Math.min(changes.hits, changes.shots) : 0

                // Track max of 300 damage per kill. Somewhat arbitrary
                changes.damage =
                    m(changes.damage) ? Math.min(changes.damage, changes.kills * 300) : 0

                // Impossible to plant more than once per round
                changes.plants =
                    m(changes.plants) ? Math.min(changes.plants, changes.rounds) : 0

                // Impossible to defuse and not win, therefore impossible that defuse > wins
                changes.defuse =
                    m(changes.defuse) ? Math.min(changes.defuse, changes.rwins) : 0

                // Impossible to rescue and not win, ignores multiple rescues intentionally
                changes.hostage =
                    m(changes.hostage) ? Math.min(changes.hostage, changes.rwins) : 0

                // 10 score per minute. Somewhat arbitrary
                changes.contrib =
                    m(changes.contrib) ? vpm(changes.contrib, changes.time, 10) : 0

                // $3,000 per minute. Somewhat arbitrary
                changes.money =
                    m(changes.money) ? vpm(changes.money, changes.time, 3000) : 0

                // Impossible to get more MVP than round wins
                changes.mvps =
                    m(changes.mvps) ? Math.min(changes.mvps, changes.rwins) : 0

                // Impossible to get more knife kills than kills
                changes.knife =
                    m(changes.knife) ? Math.min(changes.knife, changes.kills) : 0

                // Impossible to get more nade kills than kills
                changes.nades =
                    m(changes.nades) ? Math.min(changes.nades, changes.kills) : 0

                // Impossible to get more flame kills than kills
                changes.fires =
                    m(changes.fires) ? Math.min(changes.fires, changes.kills) : 0

                // Impossible to get more flash kills than kills
                changes.flash =
                    m(changes.flash) ? Math.min(changes.flash, changes.kills) : 0

                // ...
                changes.csnipe =
                    m(changes.csnipe) ? Math.min(changes.csnipe, changes.kills) : 0

                // ...
                changes.backfire =
                    m(changes.backfire) ? Math.min(changes.backfire, changes.kills) : 0

                // It takes at least 4 consecutive kills to dominate an enemy
                changes.doms =
                    m(changes.doms) ? Math.min(changes.doms, ((changes.kills / 4) - 0.5).toFixed(0)) : 0

                // Cannot revenge more than kills
                changes.revs =
                    m(changes.revs) ? Math.min(changes.revs, changes.kills) : 0

                // Must dominate before getting overkills
                changes.overkill =
                    m(changes.overkill) ? Math.min(changes.overkill, changes.kills - changes.doms) : 0

                // Cannot win more rounds than wins
                changes.pistolwin =
                    m(changes.pistolwin) ? Math.min(changes.pistolwin, changes.rwins) : 0

                // Somewhat arbitrary
                changes.donation =
                    m(changes.donation) ? Math.min(changes.donation, changes.rounds * 5) : 0

                // Impossible to play more than rounds played
                changes.rnds_cbble =
                    m(changes.rnds_cbble) ? Math.min(changes.rnds_cbble, changes.rounds) : 0

                // Impossible to play more than rounds played
                changes.rnds_dust2 =
                    m(changes.rnds_dust2) ? Math.min(changes.rnds_dust2, changes.rounds) : 0

                // ...
                changes.rnds_inferno =
                    m(changes.rnds_inferno) ? Math.min(changes.rnds_inferno, changes.rounds) : 0

                // ...
                changes.rnds_nuke =
                    m(changes.rnds_nuke) ? Math.min(changes.rnds_nuke, changes.rounds) : 0

                // ...
                changes.rnds_train =
                    m(changes.rnds_train) ? Math.min(changes.rnds_train, changes.rounds) : 0

                // Smallest of rounds on Cobblestone or rounds won
                changes.wins_cbble =
                    m(changes.wins_cbble) ? Math.min(changes.rnds_cbble, changes.rwins) : 0

                // Smallest of rounds on Dust 2 or rounds won
                changes.wins_dust2 =
                    m(changes.wins_dust2) ? Math.min(changes.rnds_dust2, changes.rwins) : 0

                // Smallest of rounds on Inferno or rounds won
                changes.wins_inferno =
                    m(changes.wins_inferno) ? Math.min(changes.rnds_inferno, changes.rwins) : 0

                // Smallest of rounds on Nuke or rounds won
                changes.wins_nuke =
                    m(changes.wins_nuke) ? Math.min(changes.rnds_nuke, changes.rwins) : 0

                // Smallest of rounds on Train or rounds won
                changes.wins_train =
                    m(changes.wins_train) ? Math.min(changes.rnds_train, changes.rwins) : 0


                // Long gun stats

                /* These follow a very similar pattern, with the only difference being RPM.
                 * That is why there are no comments other than gun names. RPMS are slightly
                 * rounded from the max RPM, since real games also include reload times, reducing
                 * the average RPM. Also, most players do not walk around spraying constantly, so
                 * some RPMS may seem drastically lower than the actual RPM of the gun.
                 */

                // Glock-18

                changes.g0s =
                    m(changes.g0s) ? vpm(changes.g0s, changes.time, 400) : 0

                changes.g0h =
                    m(changes.g0h) ? Math.min(changes.g0h, changes.g0s) : 0

                changes.g0k =
                    m(changes.g0k) ? Math.min(changes.g0k, changes.kills) : 0

                // P2000 & USP-S
                changes.g2s =
                    m(changes.g2s) ? vpm(changes.g2s, changes.time, 300) : 0

                changes.g2h =
                    m(changes.g2h) ? Math.min(changes.g2h, changes.g2s) : 0

                changes.g2k =
                    m(changes.g2k) ? Math.min(changes.g2k, changes.kills) : 0

                // Dual Berettas
                changes.g3s =
                    m(changes.g3s) ? vpm(changes.g3s, changes.time, 450) : 0

                changes.g3h =
                    m(changes.g3h) ? Math.min(changes.g3h, changes.g3s) : 0

                changes.g3k =
                    m(changes.g3k) ? Math.min(changes.g3k, changes.kills) : 0

                // P250
                changes.g4s =
                    m(changes.g4s) ? vpm(changes.g4s, changes.time, 350) : 0

                changes.g4h =
                    m(changes.g4h) ? Math.min(changes.g4h, changes.g4s) : 0

                changes.g4k =
                    m(changes.g4k) ? Math.min(changes.g4k, changes.kills) : 0

                // Tec-9 & CZ75-Auto
                changes.g5s =
                    m(changes.g5s) ? vpm(changes.g5s, changes.time, 450) : 0

                changes.g5h =
                    m(changes.g5h) ? Math.min(changes.g5h, changes.g5s) : 0

                changes.g5k =
                    m(changes.g5k) ? Math.min(changes.g5k, changes.kills) : 0

                // Five Seven & CZ75-Auto
                changes.g6s =
                    m(changes.g6s) ? vpm(changes.g6s, changes.time, 450) : 0

                changes.g6h =
                    m(changes.g6h) ? Math.min(changes.g6h, changes.g6s) : 0

                changes.g6k =
                    m(changes.g6k) ? Math.min(changes.g6k, changes.kills) : 0

                // Desert Eagle & R8 Revolver
                changes.g8s =
                    m(changes.g8s) ? vpm(changes.g8s, changes.time, 264) : 0

                changes.g8h =
                    m(changes.g8h) ? Math.min(changes.g8h, changes.g8s) : 0

                changes.g8k =
                    m(changes.g8k) ? Math.min(changes.g8k, changes.kills) : 0

                // Galil AR
                changes.g10s =
                    m(changes.g10s) ? vpm(changes.g10s, changes.time, 600) : 0

                changes.g10h =
                    m(changes.g10h) ? Math.min(changes.g10h, changes.g10s) : 0

                changes.g10k =
                    m(changes.g10k) ? Math.min(changes.g10k, changes.kills) : 0

                // FAMAS
                changes.g11s =
                    m(changes.g11s) ? vpm(changes.g11s, changes.time, 600) : 0

                changes.g11h =
                    m(changes.g11h) ? Math.min(changes.g11h, changes.g11s) : 0

                changes.g11k =
                    m(changes.g11k) ? Math.min(changes.g11k, changes.kills) : 0

                // AK-47
                changes.g12s =
                    m(changes.g12s) ? vpm(changes.g12s, changes.time, 550) : 0

                changes.g12h =
                    m(changes.g12h) ? Math.min(changes.g12h, changes.g12s) : 0

                changes.g12k =
                    m(changes.g12k) ? Math.min(changes.g12k, changes.kills) : 0

                // M4A1-S & M4A4
                changes.g14s =
                    m(changes.g14s) ? vpm(changes.g14s, changes.time, 600) : 0

                changes.g14h =
                    m(changes.g14h) ? Math.min(changes.g14h, changes.g14s) : 0

                changes.g14k =
                    m(changes.g14k) ? Math.min(changes.g14k, changes.kills) : 0

                // SSG 08
                changes.g15s =
                    m(changes.g15s) ? vpm(changes.g15s, changes.time, 40) : 0

                changes.g15h =
                    m(changes.g15h) ? Math.min(changes.g15h, changes.g15s) : 0

                changes.g15k =
                    m(changes.g15k) ? Math.min(changes.g15k, changes.kills) : 0

                // SG 553
                changes.g16s =
                    m(changes.g16s) ? vpm(changes.g16s, changes.time, 600) : 0

                changes.g16h =
                    m(changes.g16h) ? Math.min(changes.g16h, changes.g16s) : 0

                changes.g16k =
                    m(changes.g16k) ? Math.min(changes.g16k, changes.kills) : 0

                // AUG
                changes.g17s =
                    m(changes.g17s) ? vpm(changes.g17s, changes.time, 600) : 0

                changes.g17h =
                    m(changes.g17h) ? Math.min(changes.g17h, changes.g17s) : 0

                changes.g17k =
                    m(changes.g17k) ? Math.min(changes.g17k, changes.kills) : 0

                // AWP
                changes.g18s =
                    m(changes.g18s) ? vpm(changes.g18s, changes.time, 30) : 0

                changes.g18h =
                    m(changes.g18h) ? Math.min(changes.g18h, changes.g18s) : 0

                changes.g18k =
                    m(changes.g18k) ? Math.min(changes.g18k, changes.kills) : 0

                // G3SG1
                changes.g19s =
                    m(changes.g19s) ? vpm(changes.g19s, changes.time, 200) : 0

                changes.g19h =
                    m(changes.g19h) ? Math.min(changes.g19h, changes.g19s) : 0

                changes.g19k =
                    m(changes.g19k) ? Math.min(changes.g19k, changes.kills) : 0

                // SCAR-20
                changes.g20s =
                    m(changes.g20s) ? vpm(changes.g20s, changes.time, 200) : 0

                changes.g20h =
                    m(changes.g20h) ? Math.min(changes.g20h, changes.g20s) : 0

                changes.g20k =
                    m(changes.g20k) ? Math.min(changes.g20k, changes.kills) : 0

                // MAC-10
                changes.g21s =
                    m(changes.g21s) ? vpm(changes.g21s, changes.time, 750) : 0

                changes.g21h =
                    m(changes.g21h) ? Math.min(changes.g21h, changes.g21s) : 0

                changes.g21k =
                    m(changes.g21k) ? Math.min(changes.g21k, changes.kills) : 0

                // MP9
                changes.g22s =
                    m(changes.g22s) ? vpm(changes.g22s, changes.time, 750) : 0

                changes.g22h =
                    m(changes.g22h) ? Math.min(changes.g22h, changes.g22s) : 0

                changes.g22k =
                    m(changes.g22k) ? Math.min(changes.g22k, changes.kills) : 0

                // MP7
                changes.g23s =
                    m(changes.g23s) ? vpm(changes.g23s, changes.time, 700) : 0

                changes.g23h =
                    m(changes.g23h) ? Math.min(changes.g23h, changes.g23s) : 0

                changes.g23k =
                    m(changes.g23k) ? Math.min(changes.g23k, changes.kills) : 0

                // UMP-45
                changes.g24s =
                    m(changes.g24s) ? vpm(changes.g24s, changes.time, 600) : 0

                changes.g24h =
                    m(changes.g24h) ? Math.min(changes.g24h, changes.g24s) : 0

                changes.g24k =
                    m(changes.g24k) ? Math.min(changes.g24k, changes.kills) : 0

                // P90
                changes.g25s =
                    m(changes.g25s) ? vpm(changes.g25s, changes.time, 750) : 0

                changes.g25h =
                    m(changes.g25h) ? Math.min(changes.g25h, changes.g25s) : 0

                changes.g25k =
                    m(changes.g25k) ? Math.min(changes.g25k, changes.kills) : 0

                // PP-Bizon
                changes.g26s =
                    m(changes.g26s) ? vpm(changes.g26s, changes.time, 700) : 0

                changes.g26h =
                    m(changes.g26h) ? Math.min(changes.g26h, changes.g26s) : 0

                changes.g26k =
                    m(changes.g26k) ? Math.min(changes.g26k, changes.kills) : 0

                // Nova
                changes.g27s =
                    m(changes.g27s) ? vpm(changes.g27s, changes.time, 50) : 0

                changes.g27h =
                    m(changes.g27h) ? Math.min(changes.g27h, changes.g27s) : 0

                changes.g27k =
                    m(changes.g27k) ? Math.min(changes.g27k, changes.kills) : 0

                // XM1014
                changes.g28s =
                    m(changes.g28s) ? vpm(changes.g28s, changes.time, 100) : 0

                changes.g28h =
                    m(changes.g28h) ? Math.min(changes.g28h, changes.g28s) : 0

                changes.g28k =
                    m(changes.g28k) ? Math.min(changes.g28k, changes.kills) : 0

                // Sawed-Off
                changes.g29s =
                    m(changes.g29s) ? vpm(changes.g29s, changes.time, 50) : 0

                changes.g29h =
                    m(changes.g29h) ? Math.min(changes.g29h, changes.g29s) : 0

                changes.g29k =
                    m(changes.g29k) ? Math.min(changes.g29k, changes.kills) : 0

                // MAG-7
                changes.g30s =
                    m(changes.g30s) ? vpm(changes.g30s, changes.time, 50) : 0

                changes.g30h =
                    m(changes.g30h) ? Math.min(changes.g30h, changes.g30s) : 0

                changes.g30k =
                    m(changes.g30k) ? Math.min(changes.g30k, changes.kills) : 0

                // M249
                changes.g31s =
                    m(changes.g31s) ? vpm(changes.g31s, changes.time, 700) : 0

                changes.g31h =
                    m(changes.g31h) ? Math.min(changes.g31h, changes.g31s) : 0

                changes.g31k =
                    m(changes.g31k) ? Math.min(changes.g31k, changes.kills) : 0

                // Negev
                changes.g32s =
                    m(changes.g32s) ? vpm(changes.g32s, changes.time, 700) : 0

                changes.g32h =
                    m(changes.g32h) ? Math.min(changes.g32h, changes.g32s) : 0

                changes.g32k =
                    m(changes.g32k) ? Math.min(changes.g32k, changes.kills) : 0

                // Zeus x27
                changes.g35s =
                    m(changes.g35s) ? vpm(changes.g35s, changes.time, 2) : 0

                changes.g35k =
                    m(changes.g35k) ? Math.min(changes.g35k, changes.g35s, changes.kills) : 0

                for (stat in changes)
                    if (changes.hasOwnProperty(stat) && changes[stat] > 0 && mega.hasOwnProperty(stat))
                        mega[stat].add(changes[stat])
            }

            var timePlayersEnd = Number(new Date())

            console.log(`Job ${job.id}: 'update-stats' Finished processing ${playerInfo.total} accounts in ${timePlayersEnd - timePlayersStarted}ms`)
            var midTime = Number(new Date())

            // After updating all player stats
            for (stat in mega)
                if (mega.hasOwnProperty(stat))
                    mega[stat] = mega[stat].toString() // Convert BigNumber to strings

            var date = new Date(timeStarted)

            col.findOne({steam_id: 'mega'}, (err, doc) => {
                if (err) {
                    db.close()
                    var m
                    try {
                        m = JSON.stringify(mega)
                    } catch (e) {
                        m = mega
                    }
                    mailer.send({
                        to: 'devteam@csgo-skill.com',
                        from: 'Skill Bot <skillbot@csgo-skill.com>',
                        subject: 'Job \'update-stats\' failed to store Mega Stats',
                        html: `I failed when searching for 'steam_id': 'mega', but I still have the stats.<br/>${err}<br/>${m}`
                    }).then(() => { /* Nothing to do here */ })
                    .catch(error => { console.error(`Job ${job.id}: 'update-stats' failed on Mega stats, and could not send mail to devteam: ${error} \n${m}`) })
                    return
                }
                var newMega = {}
                for (stat in mega)
                    if (mega.hasOwnProperty(stat))
                        newMega[stat] = new BigNumber(doc.stats[stat] || 0).add(mega[stat]).toString()

                col.findOneAndUpdate({steam_id:'mega'}, {$set:{'stats':newMega}}, (err, _) => {
                    db.close()
                    if (err) {
                        var m
                        try {
                            m = JSON.stringify(mega)
                        } catch (e) {
                            m = mega
                        }
                        mailer.send({
                            to: 'devteam@csgo-skill.com',
                            from: 'Skill Bot <skillbot@csgo-skill.com>',
                            subject: 'Job \'update-stats\' failed to store Mega Stats',
                            html: `I failed when updating 'steam_id': 'mega', but I still have the stats.<br/>${err}<br/>${m}`
                        }).then(() => { /* Nothing to do here */ })
                        .catch(error => { console.error(`Job ${job.id}: 'update-stats' failed on Mega stats, and could not send mail to devteam: ${error} \n${m}`) })
                        return
                    }
                    console.log(`Job ${job.id}: 'update-stats' Updated the mega stats in ${Number(new Date()) - midTime}ms`)
                    mailer.send({
                        to: 'devteam@csgo-skill.com',
                        from: 'Skill Bot <skillbot@csgo-skill.com>',
                        subject: `Mega Stats for ${date.getDate()}/${date.getMonth() + 1}/${date.getFullYear()}`,
                        html: ejs.render(template.email.devmegastats, {'mega':mega, 'activePlayers': playerInfo.active})
                    }).then(() => { /* Nothing to do here */ })
                    .catch(error => { console.error(`Job ${job.id}: 'update-stats' sendMail to devteam failed: ${error}`) })
                })
            })

            var timeEnded = Number(new Date())

            var timeTotal = timeEnded - timeStarted
            var millis = timeTotal % 1000
            var seconds = ((timeTotal - millis) / 1000).toFixed(0)
            var minutes = (seconds / 60).toFixed(0)
            seconds %= 60
            var timef = (minutes > 0) ? minutes + 'm' : ''
            timef += (seconds > 0) ? ((timef.length > 0) ? ' ' : '') + seconds + '.' : ''
            timef += (seconds > 0) ? millis + 's' : ((timef.length > 0) ? ' ' : '') + millis + 'ms'

            // Amazing time formatting above B-)

            mailer.send({
                to: 'devteam@csgo-skill.com',
                from: 'Skill Bot <skillbot@csgo-skill.com>',
                subject: `Stat Updates for ${date.getDate()}/${date.getMonth() + 1}/${date.getFullYear()}`,
                html: ejs.render(template.email.devstats, {
                    'time': timef,
                    'info': playerInfo
                })
            }).then(() => { /* Nothing to do here */ })
            .catch(error => { console.error(`Job ${job.id}: 'update-stats' sendMail to devteam failed: ${error}`) })
            // 1337!
            skill.db('collection:StatLogs', (err, db, col) => {

                var logDump = {
                    'date': new Date(),
                    'time': Number(timeTotal),
                    'timePlayers': timePlayersEnd - timePlayersStarted,
                    'total': playerInfo.total,
                    'active': playerInfo.active,
                    'checked': playerInfo.checked,
                    'skipped': playerInfo.skipped,
                    'changes': playerInfo.changes,
                    'noChanges': playerInfo.noChanges,
                    'updated': playerInfo.updated,
                    'updatedNoChanges': playerInfo.updatedNoChanges,
                    'totalFailedRequests': playerInfo.failedRequests.length,
                    'failedRequests': playerInfo.failedRequests,
                    'totalBadRequests': playerInfo.badRequests.length,
                    'badRequests': playerInfo.badRequests,
                    'totalBadStats': playerInfo.badStats.length,
                    'badStats': playerInfo.badStats,
                    'totalBadUpdates': playerInfo.badUpdate.length,
                    'badUpdates': playerInfo.badUpdate,
                }

                if (err) {
                    db.close()
                    var m
                    try {
                        m = JSON.stringify(logDump)
                    } catch (e) {
                        m = logDump
                    }
                    mailer.send({
                        to: 'devteam@csgo-skill.com',
                        from: 'Skill Bot <skillbot@csgo-skill.com>',
                        subject: 'Job \'update-stats\' failed to store stat log',
                        html: `I failed when connecting to StatLogs collection, but I still have the log<br/>${err}<br/>${m}`
                    }).then(() => { /* Nothing to do here */ })
                    .catch(error => { console.error(`Job ${job.id}: 'update-stats' failed on StatLog, and could not send mail to devteam: ${error} \n${m}`) })
                    return
                }

                col.insertOne(logDump, (err, _) => {
                    db.close()
                    if (err) {
                        var m
                        try {
                            m = JSON.stringify(logDump)
                        } catch (e) {
                            m = logDump
                        }
                        mailer.send({
                            to: 'devteam@csgo-skill.com',
                            from: 'Skill Bot <skillbot@csgo-skill.com>',
                            subject: 'Job \'update-stats\' failed to store stat log',
                            html: `I failed when inserting into the StatLogs collection, but I still have the log<br/>${err}<br/>${m}`
                        }).then(() => { /* Nothing to do here */ })
                        .catch(error => { console.error(`Job ${job.id}: 'update-stats' failed on StatLog, and could not send mail to devteam: ${error} \n${m}`) })
                    }
                })

            })

            console.log(`Job ${job.id}: 'update-stats' finished processing in ${timeEnded - timeStarted}ms`)
            done()

        })
    })
}

console.log(`Worker Started in ${Number(new Date()) - start}ms`)

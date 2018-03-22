const skill = require('./mongo.js')

require('dotenv').config()
const MONGOURL = process.env.MONGOURL

skill.connect(MONGOURL)
skill.db('collection:Stats', (error, col) => {
    if (error) throw error
    col.insertOne({
        steam_id: 'mega',
        start: +new Date(),
        stats: {
        kills: '0',
        deaths: '0',
        time: '0',
        heads: '0',
        rwins: '0',
        rounds: '0',
        mwins: '0',
        matches: '0',
        shots: '0',
        hits: '0',
        damage: '0',
        plants: '0',
        defuse: '0',
        hostage: '0',
        contrib: '0',
        money: '0',
        mvps: '0',
        knife: '0',
        nades: '0',
        fires: '0',
        csnipe: '0',
        backfire: '0',
        doms: '0',
        revs: '0',
        overkill: '0',
        pistolwin: '0',
        donation: '0',
    
        wins_cbble: '0',
        wins_dust2: '0',
        wins_inferno: '0',
        wins_nuke: '0',
        wins_train: '0',
    
        rnds_cbble: '0',
        rnds_dust2: '0',
        rnds_inferno: '0',
        rnds_nuke: '0',
        rnds_train: '0',
    
    //  Glock-18                          P2000 & USP-S
        g0s: '0',                         g2s: '0',
        g0h: '0',                         g2h: '0',
        g0k: '0',                         g0k: '0',
    //  Dual Berettas                     P250
        g3s: '0',                         g4s: '0',
        g3h: '0',                         g4h: '0',
        g3k: '0',                         g4k: '0',
    //  Tec-9 & CZ75-Auto                 Five Seven & CZ75-Auto
        g5s: '0',                         g6s: '0',
        g5h: '0',                         g6h: '0',
        g5k: '0',                         g6k: '0',
    //  Desert Eagle & R8 Revolver        Galil AR
        g8s: '0',                         g10s: '0',
        g8h: '0',                         g10h: '0',
        g8k: '0',                         g10k: '0',
    //  FAMAS                             AK-47
        g11s: '0',                        g12s: '0',
        g11h: '0',                        g12h: '0',
        g11k: '0',                        g12k: '0',
    //  M4A1-S & M4A4                     SSG 08
        g14s: '0',                        g15s: '0',
        g14h: '0',                        g15h: '0',
        g14k: '0',                        g15k: '0',
    //  SG 553                            AUG
        g16s: '0',                        g17s: '0',
        g16h: '0',                        g17h: '0',
        g16k: '0',                        g17k: '0',
    //  AWP                               G3SG1
        g18s: '0',                        g19s: '0',
        g18h: '0',                        g19h: '0',
        g18k: '0',                        g19k: '0',
    //  SCAR-20                           MAC-10
        g20s: '0',                        g21s: '0',
        g20h: '0',                        g21h: '0',
        g20k: '0',                        g21k: '0',
    //  MP9                               MP7
        g22s: '0',                        g23s: '0',
        g22h: '0',                        g23h: '0',
        g22k: '0',                        g23k: '0',
    //  UMP-45                            P90
        g24s: '0',                        g25s: '0',
        g24h: '0',                        g25h: '0',
        g24k: '0',                        g25k: '0',
    //  PP-Bizon                          Nova
        g26s: '0',                        g27s: '0',
        g26h: '0',                        g27h: '0',
        g26k: '0',                        g27k: '0',
    //  XM1014                            Sawed-Off
        g28s: '0',                        g29s: '0',
        g28h: '0',                        g29h: '0',
        g28k: '0',                        g29k: '0',
    //  MAG-7                             M249
        g30s: '0',                        g31s: '0',
        g30h: '0',                        g31h: '0',
        g30k: '0',                        g31k: '0',
    //  Negev                             Zeus x27
        g32s: '0',                        g35s: '0',
        g32h: '0',                        g35k: '0',
        g32k: '0'
        }
    }, (err, result) => {
        if (err) throw err
        console.log(result)
    })
})

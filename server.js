  /********************************************************
  *    This file is licensed under the MIT 2.0 license    *
  *           Last updated February 23rd, 2018            *
  *   *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *   *
  *    Please check out the full repository located at    *
  *   http://github.com/almic/CSGO-Skill for some other   *
  * important things like the User Agreement and Privacy  *
  *  Policy, as well as some helpful information on how   *
  *     you can contribute directly to the project :)     *
  ********************************************************/

/* BEGIN MODULES */
const express = require('express')
const mongojs = require('mongojs')
const crypto = require('crypto')

/** END MODULES **/

/* BEGIN SETUP */
const app = express()

/*
  @Contributors, be sure you change this to your local
  connection url, and back to process.env.MONGOURL before
  submitting a pull request!
  For help with setting up a local MongoDB, see the wiki on
  the repository linked at the top of this file.
 */
const MONGOURL = process.env.MONGOURL

/** END SETUP **/


/* BEING ROUTING */
app.get('/', (req, res) => {
    res.send('Hello World!')
})
/** END ROUTING **/

const server = app.listen(process.env.PORT || 8080, () => {
    console.log('Listening on port ' + server.address().port)
    var db = mongojs(MONGOURL)
})

// Don't forget to change the updated date!

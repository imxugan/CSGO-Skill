  /********************************************************
  *    This file is licensed under the MIT 2.0 license    *
  *           Last updated February 10th, 2018            *
  *   *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *   *
  *    Please check out the full repository located at    *
  *   http://github.com/almic/CSGO-Skill for some other   *
  * important things like the User Agreement and Privacy  *
  *  Policy, as well as some helpful information on how   *
  *     you can contribute directly to the project :)     *
  ********************************************************/

var express = require('express')
var app = express()

app.get('/', (req, res) => {
    res.send('Hello World!')
})

app.listen(process.env.PORT || 8080, () => console.log('Listening on port 3000!'))

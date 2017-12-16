<?php

require_once("setup.php");
// Reset timezone to UTC because stupid server runs cronjobs with UTC, sad!
date_default_timezone_set("UTC");

// For unexpected results
$devMails = "mickxashton@gmail.com, mick.ashton@flare-esports.net, " .
            "devteam@csgo-skill.com";
// For normal/ non-serious results
$logMail = "devteam@csgo-skill.com";

$headers  = "From: Skill Bot <skillbot@csgo-skill.com>\r\nTo: " . $devMails . "\r\n";
$headers .= "MIME-Version: 1.0\r\nContent-Type: text/html; charset=iso-8859-1\r\n";

/******  ******  ******  ******  ******  ******  ******  ******  ******  ******
 *      Check the current date & time to see what functions should run.       *
 *                                                                            *
 * Every day                                   Update daily entries for users *
 *                                                                            *
 * Every month                              Update monthly entries, combining *
 *                                       stats older than 3 months for users, *
 *                                       or removing stats > 1 month for nons *
 ******  ******  ******  ******  ******  ******  ******  ******  ******  ******/

/**
 * We'll assume this runs twice per day. Once at 1:00 MST, and again at 6:00. To
 * prevent over computing and screwing up stats by running at the wrong times,
 * it will "flip" a switch every time using a special row in the Stats table.
 *
 * At 1:00 (8:00 UTC), it makes sure the switch is set to OFF. If it is, the
 * switch flips ON and all stats are refreshed.
 *
 * At 6:00 (13:00 UTC), it checks that the switch is set to ON, and flips it OFF.
 *
 * If the switch doesn't match what the script is expecting, it should  send an
 * email to EVERYONE about it. This should NEVER happen, but just in case.
 *
 * On the 1st of each month, this will collate monthly stats at 6:00.
 */

function cron_log($message) {
    echo date("[Y-m-d H:i:s] ") . $message . "\r\n";
}

// To check the dates
[$d, $m, $y, $h] = explode(".", date("d.m.Y.H"));

if (!in_array($h, array("08","13"))) {
    mail($logMail, 'recordStats Called',
         "Called outside operating times, nothing was done.", $headers);
    cron_log("Called outside of operating times");
    exit();
}

require_once("dbInf.php");
$conn = mysqli_connect(DB_SERVER, USERNAME, PASSWORD, FLAREDB);
if ($conn->connect_error) {
    mail($devMails, 'recordStats Failed!',
         "Connection to the Database failed! Please assist!\r\n<br/>" .
         $conn->connect_error . ": " . $conn->connect_error, $headers);
    cron_log("Failed to connect to MySQL Database '" . FLAREDB . "' with error (" .
        $conn->connect_errno . "): " . $conn->connect_error);
    die();
}

function prepare_stats_db($mode) {
    global $conn, $logMail, $headers;
    $query = "INSERT INTO `Stats_01` (`id`,`recent_visits`) VALUES (\"recording\", \"$mode\")";
    if ($conn->query($query)) {
        mail($logMail, 'recordStats Setup!',
             "The recordStats script was setup.",
             $headers);
        cron_log("Successfully setup");
    } else {
        mail($logMail, 'recordStats Failed Setup!',
             "The recordStats script failed to setup.\r\n<br/>" . print_r($conn->error_list, true)),
             $headers);
        cron_log("Setup failed!");
        die();
    }
}

if ($h === "08") {
    $mailFailedQuery = false;
    $query = "SELECT `recent_visits` FROM `Stats_01` WHERE `id`=\"recording\"";
    $result = $conn->query($query);
    if ($result->num_rows === 0) {
        prepare_stats_db("OFF");
        // Might exit after calling.
    } else {
        $result = $result->fetch_assoc();
        if ($result["recent_visits"] !== "OFF") {
            mail($devMails, 'recordStats Failed!', "Switch was not OFF! Please assist!", $headers);
            cron_log("Switch was not OFF!");
            die();
        } else {
            $query = "UPDATE `Stats_01` SET `recent_vists`=\"ON\" WHERE `id`=\"recording\"";
            if (!$conn->query($query)) {
                mail($devMails, 'recordStats Failed!', "Could not flip the switch to ON! Please assist!", $headers);
                cron_log("Could not flip switch ON!");
                die(); // I don't want to risk it
            }
        }
    }

    // Switch is OFF at this point.
    require_once("convertSteamAPI.php");
    $query = "SELECT `id`, `type`, `last_time`, `recent_visits`, `global`, `history` FROM `Stats_01`";
    $result = $conn->query($query);
    $success = 0;
    $fails = 0;
    $unmet = 0;
    while ($row = $result->fetch_object()) {
        // Here's the juicy bit.
        $last_time = new DateTime($row->last_time);
        $monthago = new DateTime("-1 month");
        if ($monthago > $last_time) {
            // Older than one month
            $visits = 0;
            $recent = json_decode($row->recent_visits);
            foreach ($recent->recent_visits as $visit) {
                $visit = new DateTime($visit->date);
                if ($visit > $monthago) {$visits++;}
            }
            if ($visits < 5) {$unmet++; continue;} // Not active in last month and < 5 visits in last month
        }

        // Time to update
        $global_stats = json_decode($row->global);
        $history_stats = json_decode($row->history);
        $last_stats = $history_stats->today;
        $now_stats = downloadStats($row->id);
        if (!isset($json->playerstats)) {
            // ???
            cron_log("Profile for ($row->id) did not return any stats info!");
            $fails++;
            continue;
        }
        $now_stats = convertStats($now_stats->playerstats->stats);
        $history_stats->today = $now_stats;
        $entry = json_decode("{}");
        $check = false;
        // Check once to reduce the need to check potentially 50+ times per account
        if ($row->type === "none") {
            foreach ($now_stats as $k => $v) {
                $diff = $v - $last_stats->{$k};
                if ($diff !== 0) {
                    $entry->{$k} = $diff;
                    $check = true;
                }
            }
        } else {
            foreach ($now_stats as $k => $v) {
                $diff = $v - $last_stats->{$k};
                $global_stats->{$k} = $global_stats->{$k} + $diff;
                if ($diff !== 0) {
                    $entry->{$k} = $diff;
                    $check = true;
                }
            }
        }
        if ($check) {
            $entry->{"date"} = "$y-$m-$d";
            array_unshift($history_stats->entries_month, $entry);
        }
        if ($row->type === "none") {
            $index = 0;
            foreach ($history_stats->entries_month as $e) {
                if (new DateTime($e->date) < $monthago) {
                    unset($history_stats->entries_month[$index]);
                } $index++;
            }
        } else {
            // Time to collate previous entries, only after 3 months though!
            // We do this by gradually adding >3 month old data to a [new] year entry.
            $limit = new DateTime("-3 month");
            foreach ($history_stats->entries_month as $e) {
                $date = new DateTime($e->date);
                if ($date < $limit) {
                    [$y2, $m2] = explode(".", $date->format("Y.m"));
                    if (new DateTime($history_stats->entries_year[0]->date) != new DateTime("$y-$m")) {
                        // New entry
                        array_unshift($history_stats->entries_year, json_decode("{\"date\":\"$y-$m\"}"));
                    }
                    foreach ($e as $k => $v) {
                        if ($k === "date") {continue;} // Skip date
                        if (isset($history_stats->entries_year[0]->{$k})) {
                            $history_stats->entries_year[0]->{$k} += $v;
                        } else {
                            $history_stats->entries_year[0]->{$k} = $v;
                        }
                    }
                }
            }
            // I hope that worked lol
        }

        /**
         * Because we plan on storing monthly entries for 5 years, which is a
         * damn long time, and many users may not even stay active for 5 straight
         * years, we'll just store them indefinitely. And if we get data-fucked,
         * then we can run a clean-up script later should it be needed.
         *
         * But that probably won't happen anyway.
         */

        $query = "UPDATE `Stats_01` SET " .
                 "`current` = " . jsonToSql($now_stats) .
                 ", `global` = " . jsonToSql($global_stats) .
                 ", `history` = " . jsonToSql($history_stats) .
                 " WHERE `id` = \"$row->id\"";
        if (!$conn->query($query)) {
            $fails++;
            cron_log("Query to update stats for ($row->id) failed! Here's what we got: " . print_r($conn->error_list, true));
            if (!$mailFailedQuery) {
                mail($devMails, 'recordStats Failed!', "Update query failed at least once! Please assist!\r\n<br/>" . print_r($conn->error_list, true),
                     $headers);
                $mailFailedQuery = true;
            }
        } else { $success++; }
    }
    $total = $success + $unmet + $fails;

    if (!$mailFailedQuery) { // No failures!
        cron_log("Successfully and uneventfully finished Stat updates! Hooray!");
        mail($logMail, 'recordStats Completed', "Found $total total profiles, " .
             "of which $unmet did not meet activity requirement.\r\n<br/>Updated: " .
             "$success profiles, while $fails profiles failed to update because " .
             "no response was returned by Steam (likely private accounts).\r\n" .
             "<br/><br/>See the logs for more info.", $headers);
    } else { // We got errors :(
        cron_log("Finished Stat updates with some errors! Boo.");
        mail($logMail, 'recordStats Finished with Errors!', "Found $total total profiles, " .
             "of which $unmet did not meet activity requirement.\r\n<br/>Updated: " .
             "$success profiles, while $fails profiles failed to update due to " .
             "various reasons.\r\n<br/><br/>See the logs for more info.", $headers);
    }

} else if ($h === "13") {
    $query = "SELECT `recent_visits` FROM `Stats_01` WHERE `id`=\"recording\"";
    $result = $conn->query($query);

    if ($result->num_rows === 0) {
        prepare_stats_db("ON");
        // Might exit after calling.
    } else {
        $result = $result->fetch_assoc();
        if ($result["recent_visits"] !== "ON") {
            mail($devMails, 'recordStats Failed!', "Switch was not ON! Please assist!", $headers);
            cron_log("Switch was not ON!");
            die();
        } else {
            $query = "UPDATE `Stats_01` SET `recent_vists`=\"OFF\" WHERE `id`=\"recording\"";
            if (!$conn->query($query)) {
                mail($devMails, 'recordStats Failed!', "Could not flip the switch to ON! Please assist!", $headers);
                cron_log("Could not flip switch ON!");
                die(); // I don't want to risk it
            }
        }
    }

} else {
    // I don't even know how but sure
    mail($logMail, 'recordStats Called',
         "Called outside operating times, nothing was done.", $headers);
    cron_log("Called outside of operating times");
    exit();
}

?>

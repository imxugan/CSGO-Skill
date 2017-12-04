<?php

// Don't touch anything here
header("Content-Type: application/atom+xml");
function getCategory($category) {
    $l = strpos($category, "[");
    if ($l === false) { return 'term="'.$category.'"'; }
    $t = substr($category, 0, $l);
    $l = substr($category, $l+1, -1);
    return 'term="'.$t.'" label="'.$l.'"';
}
function buildAuthors($authors) {
    $authors = json_decode($authors);
    $s = "";
    foreach ($authors->authors as $a) {
        $s .= "        <author>\r\n";
        $s .= "            <name>" . $a->name . "</name>\r\n";
        if (isset($a->email)) {
        $s .= "            <email>" . $a->email . "</email>\r\n";
        }
        if (isset($a->uri)) {
        $s .= "            <uri>" . $a->uri . "</uri>\r\n";
        }
        $s .= "        </author>\r\n";
    }
    foreach ($authors->contributors as $c) {
        $s .= "        <contributor>\r\n";
        $s .= "            <name>" . $a->name . "</name>\r\n";
        if (isset($a->email)) {
        $s .= "            <email>" . $a->email . "</email>\r\n";
        }
        if (isset($a->uri)) {
        $s .= "            <uri>" . $a->uri . "</uri>\r\n";
        }
        $s .= "        </contributor>\r\n";
    }
    return $s;
}
require_once("dbInf.php");
$conn = mysqli_connect(DB_SERVER, USERNAME, PASSWORD, UPDATEDB);
if ($conn->connect_error) {
    error_log("1612 - Failed to connect to MySQL Database '" . UPDATEDB . "' with error (" . $conn->connect_errno . "): " . $conn->connect_error);
    die('{"success":false,"error":"1612"}');
}
$query = "SELECT `updated` FROM `News` ORDER BY `updated` DESC";
$result = $conn->query($query);
if ($result->num_rows !== 0) {
    $result = $result->fetch_assoc();
    $UPDATED = date(DATE_ATOM, $result["updated"]);
} else {
    $UPDATED = date(DATE_ATOM);
}

// Defaults, all are TEXT type!
$MAXENTRIES = 15; // The maximum number of entries to show. Should be small.
$TITLE = "Train CSGO News";
$LINK = "http://flare-esports.net/";
$SELF = "atom";
$AUTHOR = "Flare Dev Team";
$GENERATOR = "Flare.Atom";
$GEN_VERSION = "1.0";
// Syntax of categories: "term[Label],term2[Label2]"
// Everything between the outer brackets [] is the Label.
// Commas and Double Quotes MUST be HTML escaped.
$CATEGORY = "technology[Technology],gaming[Gaming/ Games],csgo[CS: GO]";
$ICON = ""; // Should be square (1:1)
$LOGO = ""; // Should be twice as wide as it is tall. (2:1)
$RIGHTS = "Copyright (c) 2017, Flare E-Sports";
$SUB = "Official Train CSGO News Feed";

?>
<?xml version="1.0" encoding="utf-8"?>
<feed xmlns="http://www.w3.org/2005/Atom" xml:lang="en">

    <title><?= $TITLE ?></title>
    <link rel="self" href="<?= $LINK.$SELF ?>" />
    <link rel="alternate" href="<?= $LINK ?>" />
    <rights><?= $RIGHTS ?></rights>
    <generator uri="<?= $LINK ?>" version="<?= $GEN_VERSION ?>">
        <?= $GENERATOR ?>
    </generator>
    <updated><?= $UPDATED ?></updated>
    <author>
        <name><?= $AUTHOR ?></name>
    </author>
    <id><?= $LINK ?></id>
    <subtitle><?= $SUB ?></subtitle>
<?php
$cats = explode(",", $CATEGORY);
foreach ($cats as $c) {
?>
    <category <?= getCategory($c) ?>/>
<?php
}

$query = "SELECT * FROM `News` ORDER BY `published` DESC LIMIT " . $MAXENTRIES;
$result = $conn->query($query);
if ($result->num_rows !== 0) {
    while ($entry = $result->fetch_assoc()) {
?>
    <entry>
        <title><?= $entry["title"] ?></title>
        <rights><?= $RIGHTS ?></rights>
        <id><?= $LINK . $entry["perma_link"] ?></id>
        <link rel="alternate" href="/<?= $entry["perma_link"] ?>" />
        <published><?= date(DATE_ATOM, $entry["published"]) ?></published>
        <updated><?= date(DATE_ATOM, $entry["updated"]) ?></updated>
        <?= buildAuthors($entry["authors"]) ?>
<?php
        $cats = explode(",", $entry["category"]);
        foreach ($cats as $c) {
?>
        <category <?= getCategory($c) ?>/>
<?php
        }
?>
        <summary><?= $entry["summary"] ?></summary>
        <content type="xhtml" xml:lang="en">
            <div xmlns="http://www.w3.org/1999/xhtml">
                <?= $entry["short"] ?>
            </div>
        </content>
    </entry>
<?php
    }
}
?>

</feed>

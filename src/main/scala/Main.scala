import java.io.{BufferedOutputStream, BufferedWriter, File, FileOutputStream, FileWriter}
import java.net.URI
import java.text.DateFormat
import java.util.{Calendar, Date}
import java.time.LocalDate
import java.time.temporal.TemporalField

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe._
import io.circe.parser._
import io.circe.generic.auto._
import cats._
import cats.implicits._

object Main extends App {
  implicit val backend = HttpURLConnectionBackend()

  case class Word(word: String) {
    override def toString: String = word
  }

  def getWordsFromApi(url: String): List[Word] = {
    val resp = sttp.get(Uri(URI.create(url))).response(asJson[List[Word]]).send().body
      .map(_.leftMap(deserializeError => "Error during deserialization: " + deserializeError.message + "\n body was " + deserializeError.original)).flatten
    resp.right.getOrElse(throw new RuntimeException("failed to get from API: " + resp.left.get))
  }

  if (args.length != 1
    || (args.length == 1 && !args(0).matches(raw"^[\w]+$$"))) {
    // TODO improve this argument parsing to handle things like number of words to retrieve
    println("USAGE: genWordList <topic>")
  } else {
    val topic = args(0)
    val relatedWords = getWordsFromApi(s"https://api.datamuse.com/words?topics=$topic&max=500").par
    println(s"Retrieved words directly related to $topic, now getting all synonyms of those (and words likely to appear adjacent)")
    val allWords = (for {
      word <- relatedWords
    } yield {
      //      println(s"Getting derivatives of $word")
      if (!word.word.contains(" ")) {
        val leftWords = getWordsFromApi(s"https://api.datamuse.com/words/?rc=$word&max=20")
        val rightWords = getWordsFromApi(s"https://api.datamuse.com/words/?lc=$word&max=20")
        val synonyms = getWordsFromApi(s"https://api.datamuse.com/words/?ml=$word&max=20")
        List(word) ++ leftWords ++ rightWords ++ synonyms
      } else {
        List(word.copy(word.word.replaceAll(" ", "")))
      }
    }).flatten.map(_.word).map(_.replaceAll(" ", "")).seq
    println("All words downloaded, filtering out duplicates")
    val priorityWords = allWords.groupBy(identity).toSeq.sortBy(-_._2.length).map(_._1).take(800) ++ relatedWords.map(_.word.replaceAll(" ", "")).seq
    val file = File.createTempFile(s"wordlist-$topic-", ".lst")
    val os = new BufferedWriter(new FileWriter(file))
    for (word <- priorityWords) {
      os.write(word)
      os.newLine()
    }
    os.flush()
    os.close()
    println("Wordlist complete! Copy from here: " + file.toString)
  }
}


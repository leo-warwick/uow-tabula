package uk.ac.warwick.tabula.web.views

import java.util.Properties

import freemarker.core.Environment
import freemarker.template._
import javax.annotation.Resource
import org.springframework.beans.factory.annotation.Value
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.util.web.EscapingUriParser

import scala.util.matching.Regex

object UrlMethodModel {
  val pattern: Regex = "\\.[^\\.]+$".r

  def addSuffix(path: String, staticHashes: Properties): String =
    staticHashes.getProperty(path.substring("/static/".length)) match {
      case hash: String => pattern.replaceFirstIn(path, s".$hash$$0")
      case _ => path
    }
}

class UrlMethodModel extends TemplateDirectiveModel with TemplateMethodModelEx {

  // Default behaviour now is to assume provided path is relative to root, i.e. includes the servlet context.
  // So either pass the whole path as the page, OR explicitly specify context in the macro if you know it.
  var context: String = "/"

  @Value("${toplevel.url}") var toplevelUrl: String = _

  @Resource(name = "staticHashes") var staticHashes: Properties = _

  val parser = new EscapingUriParser

  private def rewrite(path: String, contextOverridden: Option[String]) = {
    val contextNoRoot = contextOverridden.getOrElse(context) match {
      case "/" => ""
      case ctx => ctx
    }

    contextNoRoot + path
  }

  override def exec(args: JList[_]): TemplateModel = {
    if (args.size >= 1) {
      val contextOverridden =
        if (args.size > 1) Option(args.get(1).toString)
        else None

      val prependTopLevelUrl =
        if (args.size > 2) args.get(2) match {
          case b: Boolean => b
          case "true" => true
          case _ => false
        } else false

      val prefix =
        if (prependTopLevelUrl) toplevelUrl
        else ""

      new SimpleScalar(prefix + encode(rewrite(args.get(0).toString, contextOverridden)))
    } else {
      throw new IllegalArgumentException("")
    }
  }

  override def execute(env: Environment,
    params: JMap[_, _],
    loopVars: Array[TemplateModel],
    body: TemplateDirectiveBody): Unit = {

    val path: String = if (params.containsKey("page")) {
      val contextOverridden =
        if (params.containsKey("context")) Option(params.get("context").toString)
        else None

      rewrite(params.get("page").toString, contextOverridden)
    } else if (params.containsKey("resource")) {
      UrlMethodModel.addSuffix(params.get("resource").toString, staticHashes)
    } else {
      throw new IllegalArgumentException("")
    }

    val writer = env.getOut
    writer.write(toplevelUrl)
    writer.write(encode(path))

  }

  def encode(url: String): String = parser.parse(url).toString

}
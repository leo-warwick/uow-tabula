package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.web.views.TextRenderer

trait MockRenderer extends TextRenderer with Mockito{
  val mockRenderer: TextRenderer = mock[TextRenderer]
  def renderTemplate(id:String,model:Any ):String = {
    mockRenderer.renderTemplate(id, model)
  }
}

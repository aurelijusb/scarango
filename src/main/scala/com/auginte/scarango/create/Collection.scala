package com.auginte.scarango.create

import spray.http.Uri
import spray.json._

case class Collection(name: String) extends Request {
  override def toJson: JsValue = JsObject(
    "name" -> JsString(name)
  )

  override val uri: Uri = Uri("/_api/collection")

  override def toString: String = s"Create.Collection($name)"
}
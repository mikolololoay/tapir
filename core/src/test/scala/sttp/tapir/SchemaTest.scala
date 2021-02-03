package sttp.tapir

import sttp.tapir.SchemaType._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SchemaTest extends AnyFlatSpec with Matchers {
  it should "modify basic schema" in {
    implicitly[Schema[String]].modifyUnsafe[String]()(_.description("test")) shouldBe implicitly[Schema[String]]
      .copy(description = Some("test"))
  }

  it should "modify product schema" in {
    val info1 = SObjectInfo("X")
    Schema(SProduct(info1, List((FieldName("f1"), Schema(SString)), (FieldName("f2"), Schema(SInteger)))))
      .modifyUnsafe[String]("f2")(_.description("test").default("f2")) shouldBe Schema(
      SProduct(info1, List((FieldName("f1"), Schema(SString)), (FieldName("f2"), Schema(SInteger).description("test").default("f2"))))
    )
  }

  it should "modify nested product schema" in {
    val info1 = SObjectInfo("X")
    val info2 = SObjectInfo("Y")

    val nestedProduct = Schema(SProduct(info2, List((FieldName("f1"), Schema(SString)), (FieldName("f2"), Schema(SInteger)))))
    val expectedNestedProduct =
      Schema(
        SProduct(info2, List((FieldName("f1"), Schema(SString)), (FieldName("f2"), Schema(SInteger).description("test").default("f2"))))
      )

    Schema(SProduct(info1, List((FieldName("f3"), Schema(SString)), (FieldName("f4"), nestedProduct), (FieldName("f5"), Schema(SBoolean)))))
      .modifyUnsafe[String]("f4", "f2")(_.description("test").default("f2")) shouldBe
      Schema(
        SProduct(
          info1,
          List((FieldName("f3"), Schema(SString)), (FieldName("f4"), expectedNestedProduct), (FieldName("f5"), Schema(SBoolean)))
        )
      )
  }

  it should "modify array elements in products" in {
    val info1 = SObjectInfo("X")
    Schema(SProduct(info1, List((FieldName("f1"), Schema(SArray(Schema(SString)))))))
      .modifyUnsafe[String]("f1", Schema.ModifyCollectionElements)(_.format("xyz")) shouldBe Schema(
      SProduct(info1, List((FieldName("f1"), Schema(SArray(Schema(SString).format("xyz"))))))
    )
  }

  it should "modify array in products" in {
    val info1 = SObjectInfo("X")
    Schema(SProduct(info1, List((FieldName("f1"), Schema(SArray(Schema(SString)))))))
      .modifyUnsafe[String]("f1")(_.format("xyz")) shouldBe Schema(
      SProduct(info1, List((FieldName("f1"), Schema(SArray(Schema(SString))).format("xyz"))))
    )
  }

  it should "modify property of optional parameter" in {
    val info1 = SObjectInfo("X")
    val info2 = SObjectInfo("Y")
    Schema(SProduct(info1, List(FieldName("f1") -> Schema(SProduct(info2, List(FieldName("p1") -> Schema(SInteger).asOption))))))
      .modifyUnsafe[Int]("f1", "p1")(_.format("xyz")) shouldBe Schema(
      SProduct(
        info1,
        List(FieldName("f1") -> Schema(SProduct(info2, List(FieldName("p1") -> Schema(SInteger).asOption.format("xyz")))))
      )
    )
  }

  it should "modify property of map value" in {
    Schema(SOpenProduct(SObjectInfo("Map", List("X")), Schema(SProduct(SObjectInfo("X"), List(FieldName("f1") -> Schema(SInteger))))))
      .modifyUnsafe[Int](Schema.ModifyCollectionElements)(_.description("test")) shouldBe Schema(
      SOpenProduct(
        SObjectInfo("Map", List("X")),
        Schema(SProduct(SObjectInfo("X"), List(FieldName("f1") -> Schema(SInteger)))).description("test")
      )
    )
  }

  it should "modify open product schema" in {
    val openProductSchema =
      Schema(SOpenProduct(SObjectInfo("Map", List("X")), Schema(SProduct(SObjectInfo("X"), List(FieldName("f1") -> Schema(SInteger))))))
    openProductSchema
      .modifyUnsafe[Nothing]()(_.description("test")) shouldBe openProductSchema.description("test")
  }

  it should "generate one-of schema using the given discriminator" in {
    val coproduct = SCoproduct(
      SObjectInfo("A"),
      List(
        Schema(SProduct(SObjectInfo("H"), List(FieldName("f1") -> Schema(SInteger)))),
        Schema(SProduct(SObjectInfo("G"), List(FieldName("f1") -> Schema(SString), FieldName("f2") -> Schema(SString)))),
        Schema(SString)
      ),
      None
    )

    coproduct.addDiscriminatorField(FieldName("who_am_i")) shouldBe SCoproduct(
      SObjectInfo("A"),
      List(
        Schema(SProduct(SObjectInfo("H"), List(FieldName("f1") -> Schema(SInteger), FieldName("who_am_i") -> Schema(SString)))),
        Schema(
          SProduct(
            SObjectInfo("G"),
            List(FieldName("f1") -> Schema(SString), FieldName("f2") -> Schema(SString), FieldName("who_am_i") -> Schema(SString))
          )
        ),
        Schema(SString)
      ),
      Some(Discriminator("who_am_i", Map.empty))
    )
  }
}

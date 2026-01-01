package it.maicol07.gamerlogue.data

import it.maicol07.spraypaintkt_annotation.Attr
import it.maicol07.spraypaintkt_annotation.ResourceSchema

@ResourceSchema("user", "users")
interface UserSchema {
    @Attr val nickname: String
    @Attr val name: String
    @Attr val firstName: String?
    @Attr val lastName: String?
    @Attr val picture: String?
    @Attr val email: String
    @Attr val emailVerifiedAt: String?
    @Attr val createdAt: String
    @Attr val updatedAt: String
}

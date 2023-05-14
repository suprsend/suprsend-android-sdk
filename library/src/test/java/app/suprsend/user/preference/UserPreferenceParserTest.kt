package app.suprsend.user.preference

import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class UserPreferenceParserTest {

    @Test
    fun preferenceTest() {
        Assert.assertEquals(Preference.OPT_IN, UserPreferenceParser.getPreference("opt_in"))
        Assert.assertEquals(Preference.OPT_IN, UserPreferenceParser.getPreference("OPT_IN"))
        Assert.assertEquals(Preference.OPT_OUT, UserPreferenceParser.getPreference("opt_out"))
        Assert.assertEquals(Preference.OPT_OUT, UserPreferenceParser.getPreference("OPT_OUT"))
    }

    @Test
    fun testFailure() {
        val userPreferences = UserPreferenceParser.parse(JSONObject())
        Assert.assertNotNull(userPreferences)
        Assert.assertTrue(userPreferences.categories.isEmpty())
        Assert.assertTrue(userPreferences.channelPreferences.isEmpty())
    }


    @Test
    fun testSuccess() {
        val userPreferences = UserPreferenceParser.parse(JSONObject(getJson()))
        Assert.assertNotNull(userPreferences)

        val category = userPreferences.categories.first()
        Assert.assertEquals("system",category.rootCategory)

        Assert.assertEquals(3,category.sections.size)
        val section = category.sections.first()
        Assert.assertEquals("",section.name)

        Assert.assertEquals(2,section.subCategories.size)
        val subCategory = section.subCategories.first()
        Assert.assertEquals("Payment and history",subCategory.name)
        Assert.assertEquals("payments-and-history",subCategory.category)
        Assert.assertEquals("Send updates related to my payment history",subCategory.description)
        Assert.assertEquals("cant_unsubscribe",subCategory.defaultPreference)
        Assert.assertEquals(Preference.OPT_IN,subCategory.preference)
        Assert.assertEquals(false,subCategory.isEditable)

        Assert.assertEquals(7,subCategory.channels.size)
        val channel = subCategory.channels.first()
        Assert.assertEquals("androidpush",channel.channel)
        Assert.assertEquals(Preference.OPT_IN,channel.preference)
        Assert.assertEquals(false,channel.isEditable)

        Assert.assertEquals(7,userPreferences.channelPreferences.size)
        val channelPreference =userPreferences.channelPreferences.first()
        Assert.assertEquals("androidpush",channelPreference.channel)
        Assert.assertEquals(false,channelPreference.isRestricted)

    }

    private fun getJson(): String {
        return """
            {
                "categories": [
                    {
                        "root_category": "system",
                        "sections": [
                            {
                                "name": null,
                                "subcategories": [
                                    {
                                        "name": "Payment and history",
                                        "category": "payments-and-history",
                                        "description": "Send updates related to my payment history",
                                        "default_preference": "cant_unsubscribe",
                                        "preference": "opt_in",
                                        "is_editable": false,
                                        "channels": [
                                            {
                                                "channel": "androidpush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "email",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "inbox",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "iospush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "sms",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "webpush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "whatsapp",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            }
                                        ]
                                    },
                                    {
                                        "name": "check preview",
                                        "category": "check-preview",
                                        "description": "refund not received follow up",
                                        "default_preference": "cant_unsubscribe",
                                        "preference": "opt_in",
                                        "is_editable": false,
                                        "channels": [
                                            {
                                                "channel": "androidpush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "email",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "inbox",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "iospush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "sms",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "webpush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "whatsapp",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            }
                                        ]
                                    }
                                ]
                            },
                            {
                                "name": "system karthick1",
                                "description": "system karthick1",
                                "subcategories": [
                                    {
                                        "name": "transactional-sub-category-karthick1",
                                        "category": "transactional-sub-category-karthick1",
                                        "description": "transactional-sub-category-karthick1",
                                        "default_preference": "cant_unsubscribe",
                                        "preference": "opt_in",
                                        "is_editable": false,
                                        "channels": [
                                            {
                                                "channel": "androidpush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "email",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "inbox",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "iospush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "sms",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "webpush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "whatsapp",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            }
                                        ]
                                    },
                                    {
                                        "name": "weekly-product-updates",
                                        "category": "weekly-product-updates",
                                        "description": "test1 recheck",
                                        "default_preference": "cant_unsubscribe",
                                        "preference": "opt_in",
                                        "is_editable": false,
                                        "channels": [
                                            {
                                                "channel": "androidpush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "email",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "inbox",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "iospush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "sms",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "webpush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "whatsapp",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            }
                                        ]
                                    },
                                    {
                                        "name": "system sub krish",
                                        "category": "system-sub-krish",
                                        "description": "system sub krish",
                                        "default_preference": "cant_unsubscribe",
                                        "preference": "opt_in",
                                        "is_editable": false,
                                        "channels": [
                                            {
                                                "channel": "androidpush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "email",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "inbox",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "iospush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "sms",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "webpush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "whatsapp",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            }
                                        ]
                                    }
                                ]
                            },
                            {
                                "name": "System section name OTP test",
                                "description": "System section name OTP",
                                "subcategories": [
                                    {
                                        "name": "System sub category name - OTP test",
                                        "category": "system-sub-category-name-otp-test",
                                        "description": "System sub category name - OTP",
                                        "default_preference": "cant_unsubscribe",
                                        "preference": "opt_in",
                                        "is_editable": false,
                                        "channels": [
                                            {
                                                "channel": "androidpush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "email",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "inbox",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "iospush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "sms",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "webpush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "whatsapp",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        "root_category": "transactional",
                        "sections": [
                            {
                                "name": "Transactional karthick1",
                                "description": "Transactional karthick1",
                                "subcategories": [
                                    {
                                        "name": "test4",
                                        "category": "test4",
                                        "description": "test4 check",
                                        "default_preference": "opt_in",
                                        "preference": "opt_in",
                                        "is_editable": true,
                                        "channels": [
                                            {
                                                "channel": "androidpush",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "email",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "inbox",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "iospush",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "sms",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "webpush",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "whatsapp",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            }
                                        ]
                                    },
                                    {
                                        "name": "Transactional 1",
                                        "category": "transactional-1",
                                        "description": "Send me updates and payments",
                                        "default_preference": "cant_unsubscribe",
                                        "preference": "opt_in",
                                        "is_editable": false,
                                        "channels": [
                                            {
                                                "channel": "androidpush",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "email",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "inbox",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "iospush",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "sms",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "webpush",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "whatsapp",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            }
                                        ]
                                    },
                                    {
                                        "name": "Test6",
                                        "category": "test6",
                                        "description": "send me updates",
                                        "default_preference": "opt_out",
                                        "preference": "opt_out",
                                        "is_editable": true,
                                        "channels": [
                                            {
                                                "channel": "androidpush",
                                                "preference": "opt_out",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "email",
                                                "preference": "opt_out",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "inbox",
                                                "preference": "opt_out",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "iospush",
                                                "preference": "opt_out",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "sms",
                                                "preference": "opt_out",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "webpush",
                                                "preference": "opt_out",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "whatsapp",
                                                "preference": "opt_out",
                                                "is_editable": false
                                            }
                                        ]
                                    }
                                ]
                            },
                            {
                                "name": "test3",
                                "description": "test3",
                                "subcategories": [
                                    {
                                        "name": "check",
                                        "category": "check",
                                        "description": "asdasd",
                                        "default_preference": "cant_unsubscribe",
                                        "preference": "opt_in",
                                        "is_editable": false,
                                        "channels": [
                                            {
                                                "channel": "androidpush",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "email",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "inbox",
                                                "preference": "opt_in",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "iospush",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "sms",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "webpush",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "whatsapp",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        "root_category": "promotional",
                        "sections": [
                            {
                                "name": null,
                                "subcategories": [
                                    {
                                        "name": "promotional 3",
                                        "category": "promotional-3",
                                        "description": "promotional 3",
                                        "default_preference": "opt_in",
                                        "preference": "opt_in",
                                        "is_editable": true,
                                        "channels": [
                                            {
                                                "channel": "androidpush",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "email",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "inbox",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "iospush",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "sms",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "webpush",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            },
                                            {
                                                "channel": "whatsapp",
                                                "preference": "opt_in",
                                                "is_editable": true
                                            }
                                        ]
                                    },
                                    {
                                        "name": "test",
                                        "category": "test",
                                        "description": "test",
                                        "default_preference": "opt_out",
                                        "preference": "opt_out",
                                        "is_editable": true,
                                        "channels": [
                                            {
                                                "channel": "androidpush",
                                                "preference": "opt_out",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "email",
                                                "preference": "opt_out",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "inbox",
                                                "preference": "opt_out",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "iospush",
                                                "preference": "opt_out",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "sms",
                                                "preference": "opt_out",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "webpush",
                                                "preference": "opt_out",
                                                "is_editable": false
                                            },
                                            {
                                                "channel": "whatsapp",
                                                "preference": "opt_out",
                                                "is_editable": false
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
                ],
                "channel_preferences": [
                    {
                        "channel": "androidpush",
                        "is_restricted": false
                    },
                    {
                        "channel": "email",
                        "is_restricted": false
                    },
                    {
                        "channel": "inbox",
                        "is_restricted": false
                    },
                    {
                        "channel": "iospush",
                        "is_restricted": false
                    },
                    {
                        "channel": "sms",
                        "is_restricted": false
                    },
                    {
                        "channel": "webpush",
                        "is_restricted": false
                    },
                    {
                        "channel": "whatsapp",
                        "is_restricted": false
                    }
                ]
            }
        """.trimIndent()
    }
}
package com.example.retrofitcrudapp

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @Test fun opensGithubValidatesInputAndReturnsToDogs() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.openGithub)).perform(click())
            onView(withText(R.string.github_title)).check(matches(isDisplayed()))
            onView(withId(androidx.appcompat.R.id.search_src_text))
                .perform(replaceText("invalid_user!"), pressImeActionButton())
            onView(withId(R.id.status)).check(matches(withText(R.string.invalid_username)))
            onView(withId(R.id.backButton)).perform(click())
            onView(withId(R.id.openGithub)).check(matches(isDisplayed()))
        }
    }
}

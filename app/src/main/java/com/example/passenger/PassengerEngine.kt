package com.example.passenger

import com.example.core.AppLanguage
import com.example.core.BiText
import com.example.graphics3d.Passenger3DRenderState
import kotlin.math.cos
import kotlin.math.sin

enum class PassengerStage(val stepNumber: Int, val title: BiText, val prompt: BiText) {
    ARRIVE_AIRPORT(1, BiText("Arrive at Airport", "বিমানবন্দরে আগমন"), BiText("Step inside Hazrat Shahjalal Terminal to begin.", "হযরত শাহজালাল টার্মিনালে প্রবেশ করুন।")),
    ENTER_TERMINAL(2, BiText("Enter Terminal", "টার্মিনালে প্রবেশ"), BiText("Walk toward the flight board and select your route.", "ফ্লাইট বোর্ডের দিকে এগিয়ে রুট নির্বাচন করুন।")),
    SELECT_FLIGHT(3, BiText("Select Flight", "ফ্লাইট নির্বাচন"), BiText("Choose your destination across Bangladesh.", "বাংলাদেশের মধ্যে আপনার গন্তব্য নির্বাচন করুন।")),
    PURCHASE_TICKET(4, BiText("Purchase Ticket", "টিকিট ক্রয়"), BiText("Confirm your ticket fare in BDAS Coins.", "বিডিএএস কয়েন দিয়ে টিকিট নিশ্চিত করুন।")),
    CHECK_IN(5, BiText("Check-In Counter", "চেক-ইন কাউন্টার"), BiText("Drop luggage and check in at the green desk.", "সবুজ ডেস্কে লাগেজ জমা ও চেক-ইন করুন।")),
    BOARDING_PASS(6, BiText("Boarding Pass", "বোর্ডিং পাস"), BiText("Pick your preferred window or aisle seat.", "পছন্দের জানালার বা আইল আসন বেছে নিন।")),
    SECURITY_CHECK(7, BiText("Security Screening", "নিরাপত্তা তল্লাশি"), BiText("Walk through the cyan security arch.", "নিরাপত্তা স্ক্যানার অতিক্রম করুন।")),
    TERMINAL_SHOPS(8, BiText("Airport Tea & Shops", "টার্মিনাল দোকান ও চা"), BiText("Optional: grab Sylheti tea or head to the gate.", "ঐচ্ছিক: সিলেটি চা নিন অথবা গেটে যান।")),
    BOARDING_GATE(9, BiText("Boarding Gate", "বোর্ডিং গেট"), BiText("Present boarding pass at Gate 4 to board.", "৪ নম্বর গেটে বোর্ডিং পাস দেখিয়ে বিমানে উঠুন।")),
    BOARD_AIRCRAFT(10, BiText("Board Aircraft", "বিমানে আরোহণ"), BiText("Step down the cabin aisle to find your row.", "বিমানের ভেতরে হেঁটে আপনার সিটের সারিতে যান।")),
    FIND_SEAT(11, BiText("Take Assigned Seat", "নির্ধারিত আসনে বসুন"), BiText("Sit down and fasten your seatbelt for departure.", "আসনে বসে সিটবেল্ট বেঁধে নিন।")),
    EXPERIENCE_TAKEOFF(12, BiText("Takeoff Experience", "উড্ডয়নের অভিজ্ঞতা"), BiText("Watch the runway drop away from your cabin window!", "জানালার বাইরে দিয়ে রানওয়ে ছেড়ে আকাশে উঠুন!")),
    CABIN_CRUISE(13, BiText("In-Flight Cabin", "আকাশপথে কেবিন"), BiText("Walk the aisle, talk to crew, or enjoy the view.", "কেবিনে হাঁটুন, ক্রুদের সাথে কথা বলুন বা দৃশ্য দেখুন।")),
    EXPERIENCE_LANDING(14, BiText("Landing Approach", "অবতরণ"), BiText("Return to seat as the aircraft touches down.", "অবতরণের জন্য আসনে ফিরে সিটবেল্ট বাঁধুন।")),
    BAGGAGE_CLAIM(15, BiText("Exit & Baggage Claim", "ব্যাগেজ সংগ্রহ"), BiText("Exit the jetbridge and claim your suitcase!", "বিমান থেকে নেমে লাগেজ সংগ্রহ করে যাত্রা শেষ করুন!"))
}

data class PassengerSessionState(
    val stage: PassengerStage = PassengerStage.ARRIVE_AIRPORT,
    val originIcao: String = "VGHS",
    val destinationIcao: String = "VGEG",
    val ticketFareCoins: Long = 1800L,
    val selectedSeat: String = "6F",
    val seatRow: Int = 6,
    val seatIsRightSide: Boolean = true,
    val seatIsWindow: Boolean = true,
    val luggageChecked: Boolean = false,
    val boardingPassCode: String = "",
    val snackPurchased: String = "",
    val crewGreeting: String = "",
    val flightProgress: Float = 0f,
    val renderState: Passenger3DRenderState = Passenger3DRenderState(),
    val journeyCompleted: Boolean = false
) {
    val isInCabin: Boolean
        get() = stage.ordinal >= PassengerStage.BOARD_AIRCRAFT.ordinal && stage != PassengerStage.BAGGAGE_CLAIM
}

class PassengerController {
    fun move(state: PassengerSessionState, forward: Float, strafe: Float, dt: Float): PassengerSessionState {
        if (state.renderState.seated) return state
        val yawRad = Math.toRadians(state.renderState.yawDeg.toDouble())
        val speed = 4.6f * dt
        val fwdX = sin(yawRad).toFloat()
        val fwdZ = -cos(yawRad).toFloat()
        val rightX = cos(yawRad).toFloat()
        val rightZ = sin(yawRad).toFloat()

        val dx = (fwdX * forward + rightX * strafe) * speed
        val dz = (fwdZ * forward + rightZ * strafe) * speed

        val minX = if (state.isInCabin) -0.45f else -16f
        val maxX = if (state.isInCabin) 0.45f else 16f
        val minZ = if (state.isInCabin) -12f else -18f
        val maxZ = if (state.isInCabin) 1.5f else 18f

        val nextX = (state.renderState.playerX + dx).coerceIn(minX, maxX)
        val nextZ = (state.renderState.playerZ + dz).coerceIn(minZ, maxZ)
        return state.copy(
            renderState = state.renderState.copy(playerX = nextX, playerZ = nextZ)
        )
    }

    fun look(state: PassengerSessionState, deltaYaw: Float, deltaPitch: Float): PassengerSessionState {
        val nextYaw = ((state.renderState.yawDeg + deltaYaw) % 360f + 360f) % 360f
        val nextPitch = (state.renderState.pitchDeg + deltaPitch).coerceIn(-45f, 45f)
        return state.copy(
            renderState = state.renderState.copy(yawDeg = nextYaw, pitchDeg = nextPitch)
        )
    }

    fun toggleThirdPerson(state: PassengerSessionState): PassengerSessionState =
        state.copy(renderState = state.renderState.copy(thirdPerson = !state.renderState.thirdPerson))

    fun crewMessage(lang: AppLanguage, item: String): String =
        if (lang == AppLanguage.BANGLA) {
            "ধন্যবাদ! আপনার জন্য $item পরিবেশন করা হয়েছে। শুভ ভ্রমণ!"
        } else {
            "Here is your $item, courtesy of Bangladesh Airline crew. Enjoy your flight!"
        }
}

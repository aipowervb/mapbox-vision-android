package com.mapbox.vision.visionevents.events.roaddescription

import android.util.Log
import com.mapbox.vision.core.buffers.RoadDescriptionDataBuffer
import com.mapbox.vision.visionevents.WorldCoordinate

/**
 * @property identifier unique identifier
 * @property lines list of lines
 * @property currentLane number of current lane
 * @property currentLaneRelativePosition relative position of car in current line. 0 means left line border, 1 - right line border.
 */
data class RoadDescription(
    val identifier: Long,
    val lines: List<Line>,
    val currentLane: Int,
    val currentLaneRelativePosition: Double
) {

    companion object {

        const val TAG = "RoadDescription"

        internal fun fromRoadDescriptionBuffer(roadDescriptionDataBuffer: RoadDescriptionDataBuffer): RoadDescription? {

            fun getWorldCoordinatesList(
                linesGeometryList: List<List<WorldCoordinate>>,
                index: Int
            ): List<WorldCoordinate> = if (linesGeometryList.size > index) {
                linesGeometryList[index]
            } else {
                emptyList()
            }

            val roadDescriptionArray = roadDescriptionDataBuffer.roadDescriptionArray
            val egoOffset = roadDescriptionArray[0]
            val visibleLeftLanes = roadDescriptionArray[1].toInt()
            val visibleRightLanes = roadDescriptionArray[2].toInt()
            val visibleReverseLanes = roadDescriptionArray[3].toInt()
            val seeLeftBorder = roadDescriptionArray[4] > 0
            val seeRightBorder = roadDescriptionArray[5] > 0
            val isValid = roadDescriptionArray[6] > 0
            val width = roadDescriptionArray[7]

            if (!isValid) {
                Log.e(TAG, "Data is not valid ")
                return null
            }

            val linesGeometryList = ArrayList<List<WorldCoordinate>>()

            if (roadDescriptionDataBuffer.linesGeometryDataArray.isNotEmpty()) {
                var index = 0
                for (i in 0..roadDescriptionDataBuffer.linesGeometryDataArray.size / 12) {

                    val lineGeometry = ArrayList<WorldCoordinate>()
                    for (j in 0..3) {
                        for (k in 0..2) {
                            val x = roadDescriptionDataBuffer.linesGeometryDataArray[index++]
                            val y = roadDescriptionDataBuffer.linesGeometryDataArray[index++]
                            val z = roadDescriptionDataBuffer.linesGeometryDataArray[index++]
                            lineGeometry.add(WorldCoordinate(x, y, z))
                        }
                    }
                    linesGeometryList.add(lineGeometry)
                }
            }


            val sameDirectionVisibleLanes = visibleLeftLanes + 1 + visibleRightLanes
            val allVisibleLanes = sameDirectionVisibleLanes + visibleReverseLanes
            val lines = ArrayList<Line>(allVisibleLanes)

            for (laneIndex in (0 until visibleReverseLanes)) {
                val leftMarkingType = when {
                    laneIndex != 0 -> MarkingType.DASHES // not left border
                    seeLeftBorder -> MarkingType.CURB
                    else -> MarkingType.UNKNOWN
                }

                val rightMarkingType = when {
                    laneIndex != visibleReverseLanes - 1 -> MarkingType.DASHES
                    else -> MarkingType.SOLID
                }

                lines.add(
                    Line(
                        width = width,
                        direction = Direction.BACKWARD,
                        leftMarking = Marking(
                            leftMarkingType,
                            worldPoints = getWorldCoordinatesList(
                                linesGeometryList,
                                laneIndex
                            )
                        ),
                        rightMarking = Marking(
                            rightMarkingType,
                            worldPoints = getWorldCoordinatesList(
                                linesGeometryList,
                                laneIndex + 1
                            )
                        )
                    )
                )
            }

            for (laneIndex in (0 until sameDirectionVisibleLanes)) {
                val leftMarkingType = when {
                    laneIndex != 0 -> MarkingType.DASHES
                    visibleReverseLanes != 0 -> MarkingType.DOUBLE_SOLID
                    seeLeftBorder -> MarkingType.CURB
                    else -> MarkingType.UNKNOWN
                }

                val rightMarkingType = when {
                    laneIndex != sameDirectionVisibleLanes - 1 -> MarkingType.DASHES
                    seeRightBorder -> MarkingType.CURB
                    else -> MarkingType.UNKNOWN
                }

                lines.add(
                    Line(
                        width = width,
                        direction = Direction.FORWARD,
                        leftMarking = Marking(
                            leftMarkingType,
                            worldPoints = getWorldCoordinatesList(
                                linesGeometryList,
                                visibleReverseLanes + laneIndex
                            )
                        ),
                        rightMarking = Marking(
                            rightMarkingType,
                            worldPoints = getWorldCoordinatesList(
                                linesGeometryList,
                                visibleReverseLanes + laneIndex + 1
                            )
                        )
                    )
                )
            }

            return RoadDescription(
                identifier = roadDescriptionDataBuffer.roadDescriptionIdentifier,
                lines = lines,
                currentLane = visibleLeftLanes + visibleReverseLanes,
                currentLaneRelativePosition = egoOffset
            )
        }
    }
}

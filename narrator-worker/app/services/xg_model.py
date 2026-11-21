import math

PITCH_LENGTH_M = 105.0
PITCH_WIDTH_M = 68.0
GOAL_WIDTH_M = 7.32


def calculate_xg(x: float, y: float) -> float:
    """
    Calculates Expected Goals (xG) from normalized pitch coordinates (0-100).
    Attacking goal is centered at (100, 50).
    """
    dx = (100.0 - x) * (PITCH_LENGTH_M / 100.0)
    dy = abs(y - 50.0) * (PITCH_WIDTH_M / 100.0)

    distance = math.hypot(dx, dy)

    half_goal = GOAL_WIDTH_M / 2.0
    if dx == 0.0:
        angle = math.pi / 2.0 if dy <= half_goal else 0.0
    else:
        angle = abs(
            math.atan2(dy + half_goal, dx) - math.atan2(dy - half_goal, dx)
        )

    # Logistic regression calibrated for realistic football xG
    logit = -1.25 - (0.11 * distance) + (1.45 * angle)
    xg = 1.0 / (1.0 + math.exp(-logit))

    return round(xg, 4)

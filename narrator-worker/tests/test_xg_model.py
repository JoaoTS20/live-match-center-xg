from app.services.xg_model import calculate_xg


def test_close_central_shot_has_higher_xg_than_long_wide_shot():
    close_central = calculate_xg(94.0, 50.0)
    long_wide = calculate_xg(70.0, 15.0)

    assert 0.0 < long_wide < close_central < 1.0


def test_xg_stays_within_probability_bounds_on_goal_line():
    on_goal_line = calculate_xg(100.0, 50.0)
    wide_goal_line = calculate_xg(100.0, 10.0)

    assert 0.0 < on_goal_line < 1.0
    assert 0.0 < wide_goal_line < on_goal_line

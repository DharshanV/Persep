package com.perception.persep.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.persep.R;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.perception.persep.data_gatherer.DataGathererManager;
import com.perception.persep.data_gatherer.GPSSensor;
import com.perception.persep.math_helpers.GeoLocationHelper;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.Comparator;
import java.util.LinkedList;

public class DataVisualization_Fragment extends Fragment implements GPSSensor.Listener
{
    private static final String TAG = "DataVisualizationFragment";


    LinkedList<DataPoint> m_LocationDataPoints;
    LinkedList<DataPoint> m_LocationErrorDataPoints;

    GraphView m_GeolocationGraph;
    GraphView m_LocationErrorGraph;
    PointsGraphSeries<DataPoint> m_LocationSeries;
    LineGraphSeries<DataPoint> m_LocationErrorSeries;

    DataGathererManager m_DataGathererManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.datavisulization_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        m_DataGathererManager = new DataGathererManager(getActivity());
        m_DataGathererManager.AddGPSListener(this);
        m_DataGathererManager.onStart();

        m_LocationDataPoints = new LinkedList<>();
        m_LocationErrorDataPoints = new LinkedList<>();
        m_LocationSeries = new PointsGraphSeries<>();
        m_LocationSeries.setSize(5.0f);

        m_LocationErrorSeries = new LineGraphSeries<>();

        m_GeolocationGraph = view.findViewById(R.id.geolocation_graph);
        m_LocationErrorGraph = view.findViewById(R.id.error_graph);

        m_GeolocationGraph.getViewport().setXAxisBoundsManual(true);
        m_GeolocationGraph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        m_GeolocationGraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);

        m_LocationErrorGraph.getViewport().setYAxisBoundsManual(true);
        m_LocationErrorGraph.getViewport().setXAxisBoundsManual(true);
//        m_LocationErrorGraph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        m_LocationErrorGraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);


        m_GeolocationGraph.addSeries(m_LocationSeries);
        m_LocationErrorGraph.addSeries(m_LocationErrorSeries);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        m_DataGathererManager.onStop();
    }

    private void AddLocationData(DataPoint point)
    {
        m_LocationDataPoints.addLast(point);
        m_LocationDataPoints.sort(Comparator.comparing(DataPoint::getX));

        DataPoint[] dataPoints = m_LocationDataPoints.toArray(new DataPoint[0]);
        m_LocationSeries.resetData(dataPoints);
        m_GeolocationGraph.getViewport().setMinX(m_LocationSeries.getLowestValueX());
        m_GeolocationGraph.getViewport().setMaxX(m_LocationSeries.getHighestValueX());


        double heading = GeoLocationHelper.Heading(point.getX(), point.getY(), -33.92438202304028, 151.234065628903);
        heading= Math.toDegrees((heading));
        heading = (360 - ((heading + 360) % 360));
        Log.d("Debug", String.valueOf(heading));
        m_LocationErrorDataPoints.add(new DataPoint(m_LocationDataPoints.size(), heading));
        DataPoint[] errorPoints = m_LocationErrorDataPoints.toArray(new DataPoint[0]);
        m_LocationErrorSeries.resetData(errorPoints);

        m_LocationErrorGraph.getViewport().setMinY(0);
        m_LocationErrorGraph.getViewport().setMaxY(360);
        m_LocationErrorGraph.getViewport().setMinX(0);
        m_LocationErrorGraph.getViewport().setMaxX(m_LocationErrorDataPoints.size());
    }

    @Override
    public void onLocationChanged(double lat, double lon)
    {
        AddLocationData(new DataPoint(lat, lon));
    }
}

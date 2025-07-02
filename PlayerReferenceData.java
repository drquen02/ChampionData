package com.championdata.cia.beans;

import com.championdata.cia.CIASettings;
import com.championdata.cia.constants.RequestVariables;
import com.championdata.cia.misc.TzCalc;
import com.championdata.cia.util.CiaJSPUtil;
import com.championdata.cia.view.*;
import com.championdata.cia.view.model.PersonView;
import com.championdata.middleware2.CacheIdentifier;
import com.championdata.middleware2.sys.data.*;
import com.championdata.middleware2.sys.data.factory.ValueFactory;
import com.championdata.cddwh2.api.parameter.AFLStatsGroupBy;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.*;

public class PlayerReferenceData
{
    Set<NumericValue> seasons = new HashSet<>( );
    Set<NumericValue> competitionLevels = new HashSet<>( );
    Map<NumericValue, StatsBeanCollection> playersMap = new HashMap<>( );
    StatsBeanCollection squadDetails = StatsBeanCollection.EMPTY_COLLECTION;
    StatsBeanCollection aflListedPlayers = StatsBeanCollection.EMPTY_COLLECTION;
    private static Logger log = LoggerFactory.getLogger( PlayerReferenceData.class );

    public PlayerReferenceData( HttpServletRequest request )
    {
        squadDetails = SquadDetView.fetch( CacheIdentifier.CIA.branch( "SQUADS" ) );
        seasons = CiaJSPUtil.fetchNumericFilterAsSet( StringUtils.defaultString( request.getParameter( "season_id" ), CIASettings.default_season ) );
        competitionLevels = CiaJSPUtil.fetchNumericFilterAsSet( StringUtils.defaultString( request.getParameter( "competition_level_id" ), "14, 101" ) );
        playersMap = AflPersonRefDataView
            .fetchByCompetitionSeason(competitionLevels.toArray(new NumericValue[0]), seasons.toArray(new NumericValue[0]) )
            .splitIntoMap( AflPersonRefDataView.PERSON_ID );
    }

    public String getPlayerDisplayName( NumericValue personId )
    {
        return playersMap.get( personId ).getFirst( true ).getValue( AflPersonRefDataView.DISPLAY_NAME ).stringValue( );
    }

    public StatsBeanCollection addPlayerData( HttpServletRequest request, StatsBeanCollection stats, RequestParameters rp, boolean ignorePosition, RecruitAgeManager recruitAgeManager )
    {
        return addAflPlayerData( request, stats, rp, ignorePosition, recruitAgeManager );
    }

    public StatsBeanCollection addAflPlayerData( HttpServletRequest request, StatsBeanCollection stats, RequestParameters rp, boolean ignorePosition, RecruitAgeManager recruitAgeManager )
    {
        List<StatsBean> beanList = new ArrayList<>( );
        int defaultSeasonId = NumberUtils.toInt(CIASettings.default_season);
        int seasonId = NumberUtils.toInt(request.getParameter("season_id"), defaultSeasonId);

        if ( rp.isRecruit( ) && ( recruitAgeManager.pruneAflListedPlayers( ) || recruitAgeManager.pruneNonAflPlayers( ) ) )
        {
            NumericValue[] competitions = { ValueFactory.intInstance( 14 ), ValueFactory.intInstance( 101 ) };
            aflListedPlayers = AflSquadListView.fetchByCompetitionSeasons( competitions, seasons.toArray(new NumericValue[0]) );
        }

        NumericField identifier = rp.isRbR( ) ? CiaCommonFields.match_id
                : stats.getFields( ).contains( CiaCommonFields.person_id ) ? CiaCommonFields.person_id
                : stats.getFields( ).contains( CiaCommonFields.player_id ) ? CiaCommonFields.player_id
                : null;

        if ( identifier == null )
        {
            throw new IllegalArgumentException( "No Player identifier field exists to add reference data on." );
        }

        for ( NumericValue personID : stats.getDistinct( CiaCommonFields.person_id ) )
        {
            StatsBeanCollection personCollection = stats.sliceBy( CiaCommonFields.person_id, personID );
            StatsBeanCollection seasonalReferenceCollection = playersMap.getOrDefault(personID, StatsBeanCollection.EMPTY_COLLECTION);

            if ( seasonalReferenceCollection.equals( StatsBeanCollection.EMPTY_COLLECTION ) )
                log.error( "Failed lookup for Person ID: " + personID.intValue( ) );

            Set<NumericValue> positions = parsePlayerPositions( request );
            if ( positions.size( ) > 0 )
            {
                seasonalReferenceCollection = seasonalReferenceCollection.sliceBy( AflPersonRefDataView.GENERAL_POSITION_ID, positions );

                if ( seasonalReferenceCollection.size( ) == 0 )
                    continue;
            }
            seasonalReferenceCollection = seasonalReferenceCollection.sortDescending( AflPersonRefDataView.SEASON_ID );

            StatsBean latestReferenceData = seasonalReferenceCollection.getFirst( true );

            LocalDate dob = TzCalc
                .dateValueGetInstance(latestReferenceData.getValue(AflPersonRefDataView.DOB))
                .getLocalDate();

            if ( !rp.isMultiSeason( ) && recruitAgeManager.isRestrictAges( ) )
                if (!rp.isMultiSeason() && recruitAgeManager.pruneBean(seasonId - dob.getYear()))
                    continue;

            if ( recruitAgeManager.pruneAflListedPlayers( ) && aflListedPlayers.size( ) > 0 )
                if ( aflListedPlayers.sliceBy( AflSquadListView.person_id, personID ).size( ) > 0 )
                    continue;

            if ( recruitAgeManager.pruneNonAflPlayers( ) && aflListedPlayers.size( ) > 0 )
                if ( aflListedPlayers.sliceBy( AflSquadListView.person_id, personID ).size( ) == 0 )
                    continue;

            for ( StatsBean personBean : personCollection )
            {
                StatsBeanBuilder beanBuilder = new StatsBeanBuilder( personBean );
                StatsBean pertinentReferenceData = personBean.contains( CiaCommonFields.season_id )
                        ? seasonalReferenceCollection.sliceBy( AflPersonRefDataView.SEASON_ID, personBean.getValue( CiaCommonFields.season_id ) ).getFirst( true )
                        : latestReferenceData;
                Set<NumericValue> requestedSquads = CiaJSPUtil.fetchNumericFilterAsSet( StringUtils.defaultString( request.getParameter( RequestVariables.SQUAD_ID.getVariableName( ) ), "-1" ) );

                if ( !ignorePosition )
                    if (pertinentReferenceData.getValue( AflPersonRefDataView.GENERAL_POSITION_CODE ).stringValue().equals("Wing") && rp.isRecruit()){
                        beanBuilder.set( CiaCommonFields.position, "Mid" );
                    }
                    else{
                        beanBuilder.set( CiaCommonFields.position, pertinentReferenceData.getValue( AflPersonRefDataView.GENERAL_POSITION_CODE ) );
                    }


                String displayName = pertinentReferenceData.getValue( AflPersonRefDataView.DISPLAY_NAME ).stringValue( );
                if ( rp.isPlayerRbR( ) )
                {
                    StatsBean fixtureBean = FixtureView.fetchByMatch( personBean.getValue( CiaCommonFields.match_id ) );
                    StatsBean personSeasonReferenceData = seasonalReferenceCollection.sliceBy( AflPersonRefDataView.SEASON_ID, fixtureBean.getValue( FixtureView.SEASON_ID ) ).getFirst( true );

                    String roundString =
                            ( rp.isMultiSeason( ) ? fixtureBean.getValue( FixtureView.SEASON_ID ).intValue( ) + " - " : "" )
                                    + String.format( "%02d", fixtureBean.getValue( FixtureView.GROUP_ROUND_NO ).intValue( ) ) + " v ";

                    int homeResult = fixtureBean.getValue( FixtureView.HOME_SCORE ).intValue( ) - fixtureBean.getValue( FixtureView.AWAY_SCORE ).intValue( );
                    if ( fixtureBean.getValue( FixtureView.HOME_SQUAD_ID ).intValue( ) == personSeasonReferenceData.getValue( AflPersonRefDataView.SQUAD_ID ).intValue( ) )
                    {
                        String result = " (" + ( homeResult > 0 ? "W" : homeResult == 0 ? "D" : "L" ) + ")";
                        roundString += fixtureBean.getValue( FixtureView.AWAY_SQUAD ).stringValue( ) + result;
                    }
                    else
                    {
                        String result = " (" + ( homeResult < 0 ? "W" : homeResult == 0 ? "D" : "L" ) + ")";
                        roundString += fixtureBean.getValue( FixtureView.HOME_SQUAD ).stringValue( ) + result;
                    }

                    beanBuilder.set( CiaCommonFields.season_round_club_result, ValueFactory.stringInstance( roundString ) );
                }
                else if ( !rp.isAllSep( ) && ( !rp.isClubSet( ) || requestedSquads.size( ) > 1 ) )
                {
                    String squadSuffix = "";
                    boolean firstSquad = true;
                    List<NumericValue> sortedSquadsBySeason = new ArrayList<>( );

                    if ( personCollection.getFields( ).contains( AFLStatsGroupBy.COMP_GROUP.getStringField( ) ) && personBean.getValue( AFLStatsGroupBy.SQUAD_ID.getNumericField( ) ).intValue( ) > 0 )
                        seasonalReferenceCollection = playersMap.get( personID ).sliceBy( AflPersonRefDataView.SQUAD_ID, personBean.getValue( AFLStatsGroupBy.SQUAD_ID.getNumericField( ) ) );

                    for ( StatsBean bean : seasonalReferenceCollection.sortBy( false, AflPersonRefDataView.SEASON_ID ) )
                    {
                        NumericValue value = bean.getValue( AflPersonRefDataView.SQUAD_ID );
                        if ( !sortedSquadsBySeason.contains( value ) )
                            sortedSquadsBySeason.add( value );

                    }

                    for ( NumericValue squadId : sortedSquadsBySeason )
                    {
                        if ( firstSquad )
                            firstSquad = false;
                        else
                            squadSuffix += ", ";

                        squadSuffix += squadDetails.sliceBy( SquadDetView.SQUAD_ID, squadId ).getFirst( true ).getValue( SquadDetView.SQUAD_CODE ).stringValue( );
                    }

                    displayName += " (" + squadSuffix + ")";
                }

                beanBuilder.set( CiaCommonFields.player_display_name, ValueFactory.stringInstance( displayName ) );
                beanBuilder.set( CiaCommonFields.fullname, pertinentReferenceData.getValue( AflPersonRefDataView.FULLNAME ) );
                beanBuilder.set( CiaCommonFields.sort_name, pertinentReferenceData.getValue( AflPersonRefDataView.SORT_NAME ) );
                beanBuilder.set( CiaCommonFields.player_surname, pertinentReferenceData.getValue( AflPersonRefDataView.SURNAME ) );

                if ( request.getParameter( "ignore_club" ) != null && !rp.isAllSep( ) && requestedSquads.size( ) > 0 )
                {
                    Set<NumericValue> playerSquads = seasonalReferenceCollection.getDistinct( AflPersonRefDataView.SQUAD_ID );

                    for ( NumericValue requestedSquad : requestedSquads )
                        if ( playerSquads.contains( requestedSquad ) )
                        {
                            beanList.add( beanBuilder.build( ) );
                            break;
                        }
                }
                else
                    beanList.add( beanBuilder.build( ) );
            }
        }

        return new StatsBeanCollection( beanList );
    }

    private Set<NumericValue> parsePlayerPositions( HttpServletRequest request )
    {
        Set<NumericValue> playerPositions = new HashSet<>( );

        if ( request.getParameter( "1100" ) != null )
            playerPositions.add( ValueFactory.intInstance( 1100 ) );
        if ( request.getParameter( "1200" ) != null )
            playerPositions.add( ValueFactory.intInstance( 1200 ) );
        if ( request.getParameter( "2000" ) != null )
            playerPositions.add( ValueFactory.intInstance( 2000 ) );
        if ( request.getParameter( "2010" ) != null )
            playerPositions.add( ValueFactory.intInstance( 2010 ) );
        if ( request.getParameter( "3100" ) != null )
            playerPositions.add( ValueFactory.intInstance( 3100 ) );
        if ( request.getParameter( "3200" ) != null )
            playerPositions.add( ValueFactory.intInstance( 3200 ) );
        if ( request.getParameter( "4000" ) != null )
            playerPositions.add( ValueFactory.intInstance( 4000 ) );
        if ( request.getParameter( "4467" ) != null )
            playerPositions.add( ValueFactory.intInstance( 4467 ) );

        return playerPositions;
    }
}
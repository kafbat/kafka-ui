import React, { Suspense } from 'react';
import { NavLink, Route, Routes, useNavigate } from 'react-router-dom';
import {
  clusterTopicAclsRelativePath,
  clusterTopicConnectorsRelativePath,
  clusterTopicConsumerGroupsRelativePath,
  clusterTopicEditRelativePath,
  clusterTopicMessagesRelativePath,
  clusterTopicSettingsRelativePath,
  clusterTopicsPath,
  clusterTopicStatisticsRelativePath,
  RouteParamsClusterTopic,
} from 'lib/paths';
import ClusterContext from 'components/contexts/ClusterContext';
import {
  ActionButton,
  ActionNavLink,
  ActionDropdownItem,
} from 'components/common/ActionComponent';
import Navbar from 'components/common/Navigation/Navbar.styled';
import useAppParams from 'lib/hooks/useAppParams';
import { Dropdown, DropdownItemHint } from 'components/common/Dropdown';
import {
  useClearTopicMessages,
  useDeleteTopic,
  useRecreateTopic,
  useTopicConnectors,
  useTopicDetails,
} from 'lib/hooks/api/topics';
import {
  Action,
  CleanUpPolicy,
  ResourceType,
  TopicMessage,
} from 'generated-sources';
import PageLoader from 'components/common/PageLoader/PageLoader';
import SlidingSidebar from 'components/common/SlidingSidebar';
import useBoolean from 'lib/hooks/useBoolean';
import { useProduceMessage } from 'lib/hooks/useProduceMessage';
import ResourcePageHeading from 'components/common/ResourcePageHeading/ResourcePageHeading';
import { TopicActionsProvider } from 'components/contexts/TopicActionsContext';

import Messages from './Messages/Messages';
import Overview from './Overview/Overview';
import Settings from './Settings/Settings';
import TopicConsumerGroups from './ConsumerGroups/TopicConsumerGroups';
import Statistics from './Statistics/Statistics';
import Edit from './Edit/Edit';
import Connectors from './Connectors/Connectors';
import SendMessage from './SendMessage/SendMessage';
import Acls from './Acls/Acls';

const Topic: React.FC = () => {
  const {
    value: isSidebarOpen,
    setFalse: closeSidebar,
    setTrue: openSidebar,
  } = useBoolean(false);

  const { messageData, setMessage, clearMessage } = useProduceMessage();

  const { clusterName, topicName } = useAppParams<RouteParamsClusterTopic>();

  const openSidebarWithMessage = (message: TopicMessage) => {
    setMessage(message);
    openSidebar();
  };

  const handleCloseSidebar = () => {
    clearMessage();
    closeSidebar();
  };

  const navigate = useNavigate();
  const deleteTopic = useDeleteTopic(clusterName);
  const recreateTopic = useRecreateTopic({ clusterName, topicName });
  const { data } = useTopicDetails({ clusterName, topicName });
  const { data: connectors = [] } = useTopicConnectors({
    clusterName,
    topicName,
  });

  const { isReadOnly, isTopicDeletionAllowed } =
    React.useContext(ClusterContext);

  const deleteTopicHandler = async () => {
    await deleteTopic.mutateAsync(topicName);
    navigate(clusterTopicsPath(clusterName));
  };

  const clearMessages = useClearTopicMessages(clusterName);
  const clearTopicMessagesHandler = async () => {
    await clearMessages.mutateAsync(topicName);
  };
  const canCleanup = data?.cleanUpPolicy === CleanUpPolicy.DELETE;
  const isConnectorsAvailable = connectors.length > 0;

  return (
    <>
      <ResourcePageHeading
        text={topicName}
        backText="Topics"
        backTo={clusterTopicsPath(clusterName)}
      >
        <ActionButton
          buttonSize="M"
          buttonType="primary"
          onClick={openSidebar}
          disabled={isReadOnly}
          permission={{
            resource: ResourceType.TOPIC,
            action: Action.MESSAGES_PRODUCE,
            value: topicName,
          }}
        >
          Produce Message
        </ActionButton>
        <Dropdown disabled={isReadOnly || data?.internal}>
          <ActionDropdownItem
            onClick={() => navigate(clusterTopicEditRelativePath)}
            permission={{
              resource: ResourceType.TOPIC,
              action: Action.EDIT,
              value: topicName,
            }}
          >
            Edit settings
            <DropdownItemHint>
              Pay attention! This operation has
              <br />
              especially important consequences.
            </DropdownItemHint>
          </ActionDropdownItem>

          <ActionDropdownItem
            onClick={clearTopicMessagesHandler}
            confirm="Are you sure want to clear topic messages?"
            disabled={!canCleanup}
            danger
            permission={{
              resource: ResourceType.TOPIC,
              action: Action.MESSAGES_DELETE,
              value: topicName,
            }}
          >
            Clear messages
            <DropdownItemHint>
              Clearing messages is only allowed for topics
              <br />
              with DELETE policy
            </DropdownItemHint>
          </ActionDropdownItem>

          <ActionDropdownItem
            onClick={recreateTopic.mutateAsync}
            confirm={
              <>
                Are you sure want to recreate <b>{topicName}</b> topic?
              </>
            }
            danger
            permission={{
              resource: ResourceType.TOPIC,
              action: [Action.MESSAGES_READ, Action.CREATE, Action.DELETE],
              value: topicName,
            }}
          >
            Recreate Topic
          </ActionDropdownItem>
          <ActionDropdownItem
            onClick={deleteTopicHandler}
            confirm={
              <>
                Are you sure want to remove <b>{topicName}</b> topic?
              </>
            }
            disabled={!isTopicDeletionAllowed}
            danger
            permission={{
              resource: ResourceType.TOPIC,
              action: Action.DELETE,
              value: topicName,
            }}
          >
            Remove Topic
            {!isTopicDeletionAllowed && (
              <DropdownItemHint>
                The topic deletion is restricted at the broker
                <br />
                configuration level (delete.topic.enable = false)
              </DropdownItemHint>
            )}
          </ActionDropdownItem>
        </Dropdown>
      </ResourcePageHeading>
      <Navbar role="navigation">
        <NavLink
          to="."
          className={({ isActive }) => (isActive ? 'is-active' : '')}
          end
        >
          Overview
        </NavLink>
        <ActionNavLink
          to={clusterTopicMessagesRelativePath}
          className={({ isActive }) => (isActive ? 'is-active' : '')}
          permission={{
            resource: ResourceType.TOPIC,
            action: Action.MESSAGES_READ,
            value: topicName,
          }}
        >
          Messages
        </ActionNavLink>
        <NavLink
          to={clusterTopicConsumerGroupsRelativePath}
          className={({ isActive }) => (isActive ? 'is-active' : '')}
        >
          Consumers
        </NavLink>
        <NavLink
          to={clusterTopicSettingsRelativePath}
          className={({ isActive }) => (isActive ? 'is-active' : '')}
        >
          Settings
        </NavLink>
        <ActionNavLink
          to={clusterTopicStatisticsRelativePath}
          className={({ isActive }) => (isActive ? 'is-active' : '')}
          permission={{
            resource: ResourceType.TOPIC,
            action: Action.ANALYSIS_VIEW,
            value: topicName,
          }}
        >
          Statistics
        </ActionNavLink>
        <ActionNavLink
          to={clusterTopicAclsRelativePath}
          className={({ isActive }) => (isActive ? 'is-active' : '')}
          permission={{
            resource: ResourceType.TOPIC,
            action: Action.ANALYSIS_VIEW,
            value: topicName,
          }}
        >
          ACLs
        </ActionNavLink>
        {isConnectorsAvailable && (
          <ActionNavLink
            to={clusterTopicConnectorsRelativePath}
            className={({ isActive }) => (isActive ? 'is-active' : '')}
            permission={{
              resource: ResourceType.TOPIC,
              action: Action.VIEW,
              value: topicName,
            }}
          >
            Connectors
          </ActionNavLink>
        )}
      </Navbar>
      <TopicActionsProvider openSidebarWithMessage={openSidebarWithMessage}>
        <Suspense fallback={<PageLoader />}>
          <Routes>
            <Route index element={<Overview />} />
            <Route
              path={clusterTopicMessagesRelativePath}
              element={<Messages />}
            />
            <Route
              path={clusterTopicSettingsRelativePath}
              element={<Settings />}
            />
            <Route
              path={clusterTopicConsumerGroupsRelativePath}
              element={<TopicConsumerGroups />}
            />
            <Route
              path={clusterTopicStatisticsRelativePath}
              element={<Statistics />}
            />
            <Route path={clusterTopicAclsRelativePath} element={<Acls />} />
            {isConnectorsAvailable && (
              <Route
                path={clusterTopicConnectorsRelativePath}
                element={<Connectors connectors={connectors} />}
              />
            )}
            <Route path={clusterTopicEditRelativePath} element={<Edit />} />
          </Routes>
        </Suspense>
      </TopicActionsProvider>
      <SlidingSidebar
        open={isSidebarOpen}
        onClose={handleCloseSidebar}
        title="Produce Message"
      >
        <Suspense fallback={<PageLoader />}>
          <SendMessage
            key={messageData ? 'with-message' : 'empty'}
            closeSidebar={closeSidebar}
            messageData={messageData}
          />
        </Suspense>
      </SlidingSidebar>
    </>
  );
};

export default Topic;
